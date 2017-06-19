package pl.ekodo.crawler.focused.engine

import java.net.MalformedURLException

import akka.actor.SupervisorStrategy.{Escalate, Resume}
import akka.actor.{Actor, ActorLogging, OneForOneStrategy, PoisonPill, Props}
import akka.routing.FromConfig
import pl.ekodo.crawler.focused.engine.Supervisor._
import pl.ekodo.crawler.focused.engine.frontier._
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Companion object for [[Supervisor]] actor.
  */
object Supervisor {

  /**
    * Input message. Requests application status.
    */
  case object GetAppStatus

  /**
    * Output type message for [[GetAppStatus]].
    */
  sealed trait AppStatus

  /**
    * Returned in case of finished crawler job.
    */
  case object Finished extends AppStatus

  /**
    * Returned in case of active crawler job.
    */
  case object Working extends AppStatus

  /**
    * Internal message, requests check the crawler status.
    */
  private case object CheckStatus

  def props(config: RuntimeConfig) = Props(new Supervisor(config))

}

/**
  * Supervisor the entry point actor for the crawler.
  * It creates all actors required for starting crawling.
  *
  * @param config runtime config
  */
private class Supervisor(config: RuntimeConfig) extends Actor with ActorLogging {

  override val supervisorStrategy =
    OneForOneStrategy() {
      case _: MalformedURLException => Resume
      case _: Exception => Escalate
    }

  private val linkScrapperProps = FromConfig.props(LinkScrapper.props())

  private val scheduler = context.actorOf(Scheduler.props(linkScrapperProps), "scheduler")

  private val policy = {
    val tldPolicy = {
      if (config.topLevelDomainPolicy.nonEmpty) Some(new TopLevelDomain(config.topLevelDomainPolicy)) else None
    }
    val containsPolicy =
    {
      if (config.containsPolicy.nonEmpty) Some(new HtmlContains(config.containsPolicy)) else None
    }
    val regexpPolicy =
    {
      if (config.regexpPolicy.nonEmpty) Some(new HtmlRegexp(config.regexpPolicy.r)) else None
    }
    val policies = Seq(tldPolicy, containsPolicy, regexpPolicy).flatten
    Policy(policies: _*)
  }

  private val indexer = context.actorOf(
    Indexer.props(config.outputDir, config.depth, config.seeds.map(_.toURL).toSet, scheduler, policy), "indexer")

  private implicit val ec: ExecutionContext = context.system.dispatcher

  private val tick = context.system.scheduler.schedule(3.second, 3.second, self, CheckStatus)

  override def receive: Receive = monitor

  private def monitor: Receive = {

    case CheckStatus =>
      scheduler ! Scheduler.GetStatus
      indexer ! Indexer.GetStatus

    case Scheduler.Status(active, closed) =>
      log.info("Scheduler status [active = {}, closed = {}]", active, closed)
      if (active == 0) {
        log.info("All domains closed, ready to shutdown")
        indexer ! Indexer.Finish
        context.become(finishing)
      }

    case Indexer.Status(indexed) =>
      log.info("Indexer status [size = {}]", indexed)
      if (indexed > config.maxLinksNumber) {
        log.info("Max number of links exceeded, ready to shutdown")
        scheduler ! PoisonPill
        indexer ! Indexer.Finish
        context.become(finishing)
      }

    case GetAppStatus =>
      sender ! Working
  }

  private def finishing: Receive = {
    case Indexer.FinishOK(graphs) =>
      context.become(finished)

    case GetAppStatus =>
      sender ! Working
  }


  private def finished: Receive = {
    case GetAppStatus =>
      sender ! Finished
  }

  override def postStop(): Unit = tick.cancel()

}
