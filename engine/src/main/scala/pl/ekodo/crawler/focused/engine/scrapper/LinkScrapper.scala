package pl.ekodo.crawler.focused.engine.scrapper

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper._

object LinkScrapper {

  case class GetLinks(url: String)

  case class ScrappedLinks(src: String, links: Set[Link])

  def props(indexer: ActorRef, scrapper: Scrapper = JsoupScrapper) =
    Props(new LinkScrapper(indexer, scrapper))

}

class LinkScrapper(indexer: ActorRef, scrapper: Scrapper) extends Actor with ActorLogging {

  override def receive: Receive = {
    case GetLinks(url) =>
      val links = scrapper.links(url)
      indexer ! ScrappedLinks(url, links)
  }

}
