package pl.ekodo.crawler.focused.engine

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props}
import akka.routing.FromConfig
import pl.ekodo.crawler.focused.engine.Supervisor.{CheckStatus, GetResults}
import pl.ekodo.crawler.focused.engine.frontier.Scheduler.{GetStatus, Status}
import pl.ekodo.crawler.focused.engine.frontier.{Indexer, Scheduler}
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Supervisor {

  def props(config: RuntimeConfig) = Props(new Supervisor(config))

  case object GetResults


  private object CheckStatus

}

class Supervisor(config: RuntimeConfig) extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: Exception =>
        Escalate
    }

  private val linkScrapperProps = FromConfig.props(LinkScrapper.props())

  private val scheduler = context.actorOf(Scheduler.props(linkScrapperProps), "scheduler")

  private val indexer = context.actorOf(Indexer.props(config.depth, config.seeds.map(_.toURL).toSet, scheduler), "indexer")

  private implicit val ec: ExecutionContext = context.system.dispatcher

  private val tick = context.system.scheduler.schedule(1.second, 1.second, self, CheckStatus)

  override def receive: Receive = monitor(None)

  private def monitor(sendResultsTo: Option[ActorRef]): Receive = {
    case CheckStatus =>
      indexer ! GetStatus

    case Status(active, closed) =>
      log.info("Current status [active = {}] [closed = {}]", active, closed)

    case GetResults =>
      context.become(monitor(Some(sender)))

  }

  override def postStop(): Unit = tick.cancel()
}
