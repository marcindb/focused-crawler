package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import kamon.Kamon
import pl.ekodo.crawler.focused.engine.frontier.Indexer.{GetStatus, Index, Policy, Status}
import pl.ekodo.crawler.focused.engine.scrapper.Link

object Indexer {

  type Policy = Link => Boolean

  case object GetStatus

  case class Status(indexed: Int)

  case class Index(src: URL, seed: URL, depth: Int, links: Set[Link])

  def props(depth: Int, seeds: Set[URL], scheduler: ActorRef, policy: Policy) =
    Props(new Indexer(depth, seeds, scheduler, policy))

}

class Indexer(maxDepth: Int, seeds: Set[URL], scheduler: ActorRef, policy: Policy) extends Actor with ActorLogging {

  private val indexSizeMetric = Kamon.metrics.counter("index-size")

  private var indexed = seeds

  private val seedIndexers = seeds.map { seed =>
    (seed, context.actorOf(SeedIndexer.props(seed)))
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
  }

  private def toVisit(index: Index): Set[Link] =
    if (index.depth > maxDepth)
      Set.empty
    else
      index.links.filter(l => !indexed.contains(l.url) && policy(l))

}
