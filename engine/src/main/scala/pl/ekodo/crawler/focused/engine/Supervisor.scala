package pl.ekodo.crawler.focused.engine

import java.net.MalformedURLException

import akka.actor.SupervisorStrategy.{Escalate, Resume}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, Props}
import akka.routing.FromConfig
import pl.ekodo.crawler.focused.engine.Supervisor._
import pl.ekodo.crawler.focused.engine.frontier.{Indexer, Scheduler}
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object Supervisor {

  def props(config: RuntimeConfig) = Props(new Supervisor(config))

  case object GetAppStatus

  sealed trait AppStatus

  case object Finished extends AppStatus

  case object Working extends AppStatus

  private case object CheckStatus

}

class Supervisor(config: RuntimeConfig) extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: MalformedURLException => Resume
      case _: Exception => Escalate
    }

  private val linkScrapperProps = FromConfig.props(LinkScrapper.props())

  private val scheduler = context.actorOf(Scheduler.props(linkScrapperProps), "scheduler")

  private val indexer = context.actorOf(
    Indexer.props(config.depth, config.seeds.map(_.toURL).toSet, scheduler), "indexer")

  private implicit val ec: ExecutionContext = context.system.dispatcher

  private val tick = context.system.scheduler.schedule(1.second, 1.second, self, CheckStatus)

  override def receive: Receive = monitor(Working)

  private def monitor(status: AppStatus): Receive = {

    case CheckStatus =>
      scheduler ! Scheduler.GetStatus
      indexer ! Indexer.GetStatus

    case Scheduler.Status(active, closed) =>
      if (active == 0) {
        log.info("All domains closed, ready to shutdown")
        context.become(monitor(Finished))
      }

    case Indexer.Status(indexed) =>
      if (indexed > config.maxLinksNumber) {
        log.info("Max number of links exceeded, ready to shutdown")
        context.become(monitor(Finished))
      }

    case GetAppStatus =>
      sender ! status

  }

  override def postStop(): Unit = tick.cancel()
}
