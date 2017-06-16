package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import akka.actor.{Actor, Props}
import pl.ekodo.crawler.focused.engine.frontier.SeedIndexer.{Connection, IndexConnections}

object SeedIndexer {

  def props(seed: URL) = Props(new SeedIndexer(seed))

  case class Connection(from: URL, to: URL)

  case class IndexConnections(connections: Set[Connection])

}

class SeedIndexer(seed: URL) extends Actor {

  var links = Set.empty[Connection]

  override def receive: Receive = {
    case IndexConnections(conns) =>
      links = links ++ conns
  }

}
