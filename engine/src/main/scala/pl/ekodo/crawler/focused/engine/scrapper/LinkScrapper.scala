package pl.ekodo.crawler.focused.engine.scrapper

import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper._

object LinkScrapper {

  case class GetLinks(url: URL, seed: URL, depth: Int)

  case class GetLinksOK(gl: GetLinks, links: Set[Link])

  case class GetLinksError(message: GetLinks, reason: Throwable)

  def props(scrapper: Scrapper = JsoupScrapper) = Props(new LinkScrapper(scrapper))

}

class LinkScrapper(scrapper: Scrapper) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.debug("Link scrapper started: {}", context.self.path.toString)
  }

  override def postStop(): Unit = {
    log.debug("Link scrapper stopped: {}", context.self.path.toString)
  }

  override def receive: Receive = {
    case gl@GetLinks(url, seed, depth) =>
      val tryLinks = scrapper.links(url)
      tryLinks.fold(
        ex => {
          log.warning("Exception occurred during scrapping {}, reason {}", gl, ex.getMessage)
          sender ! GetLinksError(gl, ex)
        },
        links => {
          log.debug("Scrapped links: {}", links.size)
          sender ! GetLinksOK(gl, links)
        }
      )
  }

}
