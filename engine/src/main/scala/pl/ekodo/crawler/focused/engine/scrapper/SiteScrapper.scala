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

object SiteScrapper {

  case class Process(url: URL, seed: URL, depth: Int)

  sealed trait Done

  case object MaxRequestsExceeded extends Done

  case object MaxErrorsExceeded extends Done

  case object NoMoreWork extends Done

  def props(config: SiteScrapperConfig, indexer: ActorRef, scrapper: ActorRef) =
    Props(new SiteScrapper(config, indexer, scrapper))

}

class SiteScrapper(config: SiteScrapperConfig, indexer: ActorRef, scrapper: ActorRef)
  extends Actor with Stash with ActorLogging {

  private implicit val ec: ExecutionContext = context.system.dispatcher

  private val MaxNumberOfErrors = 20

  private var requestsCounter = 0

  private var errorsCounter = 0

  override def preStart(): Unit = {
    context.setReceiveTimeout(5.minutes)
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
