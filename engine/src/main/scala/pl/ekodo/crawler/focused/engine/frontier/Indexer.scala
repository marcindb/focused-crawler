package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.ekodo.crawler.focused.engine.frontier.Indexer.Index
import pl.ekodo.crawler.focused.engine.scrapper.Link

object Indexer {

  case class Index(src: URL, seed: URL, depth: Int, links: Set[Link])

  def props(depth: Int, seeds: Set[URL], scheduler: ActorRef) = Props(new Indexer(depth, seeds, scheduler))

}

class Indexer(depth: Int, seeds: Set[URL], scheduler: ActorRef) extends Actor with ActorLogging {

  private var indexed = seeds

  private val seedIndexers = seeds.map { seed =>
    (seed, context.actorOf(SeedIndexer.props(seed)))
  }.toMap

  override def preStart(): Unit = {
    scheduler ! Scheduler.Register(self)
    seeds.foreach { seed =>
      scheduler ! Scheduler.Schedule(seed, 0, Set(seed))
    }
  }

  override def receive: Receive = {
    case i@Index(src, seed, depth, links) =>
      val tv = toVisit(i).map(_.url)
      if(tv.nonEmpty) {
        seedIndexers(seed) ! SeedIndexer.IndexConnections(tv.map(url => SeedIndexer.Connection(src, url)))
        scheduler ! Scheduler.Schedule(seed, depth + 1, toVisit(i).map(_.url))
        indexed = indexed ++ tv
      }
  }

  private def toVisit(index: Index): Set[Link] =
    if (index.depth >= depth)
      Set.empty
    else
      index.links.filterNot(l => indexed.contains(l.url))

}
