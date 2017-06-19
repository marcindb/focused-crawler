package pl.ekodo.crawler.focused.engine.scrapper

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Stash}
import pl.ekodo.crawler.focused.engine.frontier.Indexer
import pl.ekodo.crawler.focused.engine.scrapper.SiteScrapper._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class SiteScrapperConfig(
  domain: String,
  maxLinksVisited: Int
)

/**
  * Companion object for [[SiteScrapper]] actor
  */
object SiteScrapper {

  /**
    * Input message, requests processing of given url
    *
    * @param url   url which has to be processed
    * @param seed  seed url
    * @param depth depth related to the seed url
    */
  case class Process(url: URL, seed: URL, depth: Int)

  /**
    * Message sent to parent actor in case of finished job
    */
  sealed trait Done

  /**
    * Sent in case of max number of requests for given domain exceeded.
    * Simple prevents crawler traps.
    */
  case object MaxRequestsExceeded extends Done

  /**
    * Sent in case of mac errors exceeded (for instance http 404)
    */
  case object MaxErrorsExceeded extends Done

  /**
    * Sent if the queue of jobs is empty for specified time.
    */
  case object NoMoreWork extends Done

  /**
    * Returns props of [[SiteScrapper]] actor
    *
    * @param config   scrapper config
    * @param indexer  indexer
    * @param scrapper scrapper
    * @return         props of [[SiteScrapper]]
    */
  def props(config: SiteScrapperConfig, indexer: ActorRef, scrapper: ActorRef) =
    Props(new SiteScrapper(config, indexer, scrapper))

}

/**
  * This actor is responsible for requesting given domain. It queues messages and send requests one by one
  * in order to avoid overloading of the site. If the queue is empty for given time it assumes that the site
  * is crawled and there is no more sites to visit for given domain.
  *
  * @param config   scrapper config
  * @param indexer  indexer
  * @param scrapper scrapper
  */
private class SiteScrapper(config: SiteScrapperConfig, indexer: ActorRef, scrapper: ActorRef)
  extends Actor with Stash with ActorLogging {

  private implicit val ec: ExecutionContext = context.system.dispatcher

  private val MaxNumberOfErrors = 20

  private var requestsCounter = 0

  private var errorsCounter = 0

  override def preStart(): Unit = {
    context.setReceiveTimeout(1.minute)
    log.debug("Site scrapper started for domain: {}", config.domain)
  }

  override def postStop(): Unit = {
    log.debug("Site scrapper stopped for domain: {}", config.domain)
  }

  override def receive: Receive = idle

  private def idle: Receive = {

    case Process(url, seed, depth) =>
      if (requestsCounter < config.maxLinksVisited) {
        requestsCounter = requestsCounter + 1
        scrapper ! LinkScrapper.GetLinks(url, seed, depth)
        context.become(busy)
      } else {
        sender ! MaxRequestsExceeded
      }

    case ReceiveTimeout =>
      context.parent ! NoMoreWork

  }

  private def busy: Receive = {

    case p: Process =>
      stash()

    case LinkScrapper.GetLinksOK(gl, links) =>
      unstashAll()
      context.unbecome()
      indexer ! Indexer.Index(gl.url, gl.seed, gl.depth, links)

    case error: LinkScrapper.GetLinksError =>
      errorsCounter = errorsCounter + 1
      if(errorsCounter > MaxNumberOfErrors) {
        context.parent ! MaxErrorsExceeded
      } else {
        unstashAll()
        context.unbecome()
      }

    case ReceiveTimeout =>
      log.warning("Got receive timeout waiting for scrapper response: {}", config.domain)
      unstashAll()
      context.setReceiveTimeout(5.minutes)
      context.unbecome()

  }

}
