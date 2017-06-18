package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import kamon.Kamon
import pl.ekodo.crawler.focused.engine.frontier.Scheduler._
import pl.ekodo.crawler.focused.engine.scrapper.{SiteScrapper, SiteScrapperConfig}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


object Scheduler {

  case class Schedule(seed: URL, depth: Int, urls: Set[URL])

  case class Register(indexer: ActorRef)

  private case object SetMetrics

  def props(linkScrapperProps: Props) = Props(new Scheduler(linkScrapperProps))

  private type Domain = String

  private trait CloseReason

  private case object MaxLinks extends CloseReason

  private case object Inactive extends CloseReason

  private case object MaxErrors extends CloseReason

  private type ActiveScrappers = Map[Domain, ActorRef]

  private type ClosedScrappers = Map[Domain, CloseReason]

  private val MaxRequestsPerDomain = 1000

}

class Scheduler(linkScrapperProps: Props) extends Actor with ActorLogging {

  private implicit val ec: ExecutionContext = context.dispatcher

  private val activeSizeMetric = Kamon.metrics.gauge("active-domains-size")(0L)

  private val closedSizeMetric = Kamon.metrics.gauge("closed-domains-size")(0L)

  private val metricsTick = context.system.scheduler.schedule(1.second, 1.second, self, SetMetrics)

  private var active: ActiveScrappers = Map.empty

  private var closed: ClosedScrappers = Map.empty

  private val scrappers = context.actorOf(linkScrapperProps, "scrappers")

  override def postStop(): Unit = {
    metricsTick.cancel()
  }

  override def receive: Receive = {

    case Register(indexer: ActorRef) =>
      context.watch(indexer)
      context.become(scheduling(indexer))
  }

  private def scheduling(indexer: ActorRef): Receive = {

    case Schedule(seed, depth, urls) =>
      active = createScrappers(urls, indexer)
      urls.foreach(url => process(url, seed, depth, active))

    case SiteScrapper.MaxRequestsExceeded =>
      closeScrapper(sender, MaxLinks)

    case SiteScrapper.NoMoreWork =>
      closeScrapper(sender, Inactive)

    case SiteScrapper.MaxErrorsExceeded =>
      closeScrapper(sender, MaxErrors)

    case SetMetrics =>
      activeSizeMetric.record(active.size)
      closedSizeMetric.record(closed.size)

    case Terminated(`indexer`) =>
      log.info("Indexer unregistered")
      context.become(receive)

  }

  private def createScrappers(links: Set[URL], indexer: ActorRef): ActiveScrappers = {
    val domains = links.map(_.getHost)
    val toCreate = domains.filterNot(active.contains).filterNot(closed.contains)
    val newScrappers = toCreate.map { domain =>
      (domain, context.actorOf(
        SiteScrapper.props(SiteScrapperConfig(domain, MaxRequestsPerDomain), indexer, scrappers),
        s"scrapper-$domain"))
    }
    active ++ newScrappers
  }

  private def closeScrapper(scrapper: ActorRef, reason: CloseReason) = {
    active.find { case (_, v) => v == scrapper }.foreach { case (domain, _) =>
      active = active - domain
      closed = closed + (domain -> reason)
    }
    scrapper ! PoisonPill
  }

  private def process(url: URL, seed: URL, depth: Int, scrappers: ActiveScrappers) =
    scrappers.get(url.getHost).foreach { scrapper =>
      scrapper ! SiteScrapper.Process(url, seed, depth)
    }

}
