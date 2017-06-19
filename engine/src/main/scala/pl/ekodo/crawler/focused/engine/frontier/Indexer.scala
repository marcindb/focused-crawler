package pl.ekodo.crawler.focused.engine.frontier

import java.net.{MalformedURLException, URL}
import java.nio.file.Path

import akka.actor.SupervisorStrategy.{Escalate, Resume}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, Terminated}
import kamon.Kamon
import pl.ekodo.crawler.focused.engine.frontier.Indexer._
import pl.ekodo.crawler.focused.engine.scrapper.Link

object Indexer {

  type Policy = Link => Boolean

  case class Index(src: URL, seed: URL, depth: Int, links: Set[Link])

  case object GetStatus

  case class Status(indexed: Int)

  case object Finish

  case class FinishOK(graphs: Set[Path])

  def props(outputDir: String, depth: Int, seeds: Set[URL], scheduler: ActorRef, policy: Policy) =
    Props(new Indexer(outputDir, depth, seeds, scheduler, policy))

}

class Indexer(outputDir: String, maxDepth: Int, seeds: Set[URL], scheduler: ActorRef, policy: Policy)
  extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: MalformedURLException => Resume
      case _: Exception => Escalate
    }

  private val indexSizeMetric = Kamon.metrics.counter("index-size")

  private var indexed = seeds

  private val seedIndexers = seeds.map { seed =>
    val seedIndexer = context.actorOf(SeedIndexer.props(seed))
    context.watch(seedIndexer)
    (seed, seedIndexer)
  }.toMap

  override def preStart(): Unit = {
    scheduler ! Scheduler.Register(self)
    seeds.foreach { seed =>
      scheduler ! Scheduler.Schedule(seed, 0, Set(seed))
    }
    indexSizeMetric.increment(seeds.size)
  }

  override def receive: Receive = {
    case i@Index(src, seed, depth, links) =>
      val tv = toVisit(i).map(_.url)
      if(tv.nonEmpty) {
        seedIndexers(seed) ! SeedIndexer.IndexConnections(tv.map(url => SeedIndexer.Connection(src, url)))
        scheduler ! Scheduler.Schedule(seed, depth + 1, toVisit(i).map(_.url))
        indexed = indexed ++ tv
        indexSizeMetric.increment(tv.size)
      }

    case GetStatus =>
      sender ! Status(indexed.size)

    case Finish =>
      seedIndexers.values.foreach { seedIndexer =>
        seedIndexer ! SeedIndexer.GenerateGraph(outputDir)
      }
      context.become(finishing(sender, seedIndexers.values.toSet, Set.empty))

  }

  private def finishing(respondTo: ActorRef, waitForResponse: Set[ActorRef], graphs: Set[Path]): Receive =  {
    case SeedIndexer.GenerateGraphOK(graph) =>
      val waitFor = waitForResponse - sender
      if (waitFor.nonEmpty)
        context.become(finishing(respondTo, waitFor, graphs + graph))
      else
        respondTo ! FinishOK(graphs)
    case Terminated(ref) =>
      val waitFor = waitForResponse - ref
      if(waitFor.nonEmpty)
        context.become(finishing(respondTo, waitFor, graphs))
      else
        respondTo ! FinishOK(graphs)
  }

  private def toVisit(index: Index): Set[Link] =
    if (index.depth >= maxDepth)
      Set.empty
    else
      index.links.filter(l => !indexed.contains(l.url) && policy(l))


}
