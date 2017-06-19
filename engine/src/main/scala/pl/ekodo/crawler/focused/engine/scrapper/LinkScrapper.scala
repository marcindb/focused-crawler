package pl.ekodo.crawler.focused.engine.scrapper

import java.net.URL

import akka.actor.{Actor, ActorLogging, Props}
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper._

/**
  * Companion object for [[LinkScrapper]] actor.
  */
object LinkScrapper {

  /**
    * Input message, requests getting links from given url.
    *
    * @param url   url which has to be fetched
    * @param seed  seed url
    * @param depth depth of link related to seed url
    */
  case class GetLinks(url: URL, seed: URL, depth: Int)

  /**
    * Output message in response to [[GetLinks]], indicates successful extract of links
    *
    * @param gl     request message
    * @param links  found links
    */
  case class GetLinksOK(gl: GetLinks, links: Set[Link])

  /**
    * Output message in response to [[GetLinks]], indicates failed extract of links
    *
    * @param gl      request message
    * @param reason  reason of the failure
    */
  case class GetLinksError(gl: GetLinks, reason: Throwable)

  /**
    * Returns props of [[LinkScrapper]] actor
    *
    * @param scrapper scrapper
    * @return         props of [[LinkScrapper]]
    */
  def props(scrapper: Scrapper = JsoupScrapper) = Props(new LinkScrapper(scrapper))

}

/**
  * LinkScrapper is responsible for fetching url and extracting html anchors.
  *
  * @param scrapper scrapper
  */
private class LinkScrapper(scrapper: Scrapper) extends Actor with ActorLogging {

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
