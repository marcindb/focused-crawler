package pl.ekodo.crawler.focused.engine.scrapper

import akka.actor.{Actor, Props}
import pl.ekodo.crawler.focused.engine.scrapper.SiteScrapper._

import scala.collection.immutable.Queue
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class SiteScrapperConfig(
  domain: String,
  seedUrl: String,
  scrappingDelay: FiniteDuration,
  maxLinksVisited: Int
)

object SiteScrapper {

  case class Process(url: String)

  sealed trait DoneReason

  case object EmptyQueue extends DoneReason

  case object MaxLinksExceeded extends DoneReason

  case class Done(reason: DoneReason)

  private case object ProcessNext

  def props(config: SiteScrapperConfig, linkScrapperProps: Props) =
    Props(new SiteScrapper(config, linkScrapperProps))

}

class SiteScrapper(config: SiteScrapperConfig, linkScrapperProps: Props)
  extends Actor {

  private val linkScrapper = context.actorOf(linkScrapperProps)

  private var linksCounter = 0

  private implicit val ec: ExecutionContext = context.dispatcher

  private val tick =
    context.system.scheduler.schedule(0.millis, config.scrappingDelay, self, ProcessNext)

  override def preStart(): Unit = {
    self ! Process(config.seedUrl)
  }

  override def receive: Receive = process(Queue.empty)

  override def postStop() = tick.cancel()

  private def process(urls: Queue[String]): Receive = {
    case Process(url) =>
      if (linksCounter < config.maxLinksVisited) {
        linksCounter = linksCounter + 1
        context.become(process(urls :+ url))
      } else {
        sender ! Done(MaxLinksExceeded)
      }

    case ProcessNext =>
      urls.dequeueOption.foreach { case (url, q) =>
        linkScrapper ! LinkScrapper.GetLinks(url)
        context.become(process(q))
      }
  }

}
