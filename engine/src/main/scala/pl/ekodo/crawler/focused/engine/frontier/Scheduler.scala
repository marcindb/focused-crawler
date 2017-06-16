package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}
import pl.ekodo.crawler.focused.engine.frontier.Scheduler._
import pl.ekodo.crawler.focused.engine.scrapper.{SiteScrapper, SiteScrapperConfig}


object Scheduler {

  case class Schedule(seed: URL, depth: Int, urls: Set[URL])

  case class Register(indexer: ActorRef)

  case object GetStatus

  case class Status(activeDomains: Int, closedDomains: Int)

  def props(linkScrapperProps: Props) = Props(new Scheduler(linkScrapperProps))

  private type Domain = String

  private trait CloseReason

  private case object MaxLinks extends CloseReason

  private case object Inactive extends CloseReason

  private case object MaxErrors extends CloseReason

  private type ActiveScrappers = Map[Domain, ActorRef]

  private type ClosedScrappers = Map[Domain, CloseReason]

}

class Scheduler(linkScrapperProps: Props) extends Actor with ActorLogging {

  private var active: ActiveScrappers = Map.empty

  private var closed: ClosedScrappers = Map.empty

  private val scrappers = context.actorOf(linkScrapperProps, "scrappers")

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
      active.find { case (_, v) => v == sender }.foreach { case (domain, _) =>
        active = active - domain
        closed = closed + (domain -> MaxLinks)
      }
      sender ! PoisonPill

    case SiteScrapper.NoMoreWork =>
      active.find { case (_, v) => v == sender }.foreach { case (domain, _) =>
        active = active - domain
        closed = closed + (domain -> Inactive)
      }
      sender ! PoisonPill

    case GetStatus =>
      sender ! Status(active.size, closed.size)

    case Terminated(`indexer`) =>
      log.info("Indexer unregistered")
      context.become(receive)

  }

  private def createScrappers(links: Set[URL], indexer: ActorRef): ActiveScrappers = {
    val domains = links.map(_.getHost)
    val toCreate = domains.filterNot(active.contains).filterNot(closed.contains)
    val newScrappers = toCreate.map { domain =>
      (domain, context.actorOf(SiteScrapper.props(SiteScrapperConfig(domain, 1000), indexer, scrappers)))
    }
    active ++ newScrappers
  }

  private def process(url: URL, seed: URL, depth: Int, scrappers: ActiveScrappers) =
    scrappers.get(url.getHost).foreach { scrapper =>
      scrapper ! SiteScrapper.Process(url, seed, depth)
    }

}
