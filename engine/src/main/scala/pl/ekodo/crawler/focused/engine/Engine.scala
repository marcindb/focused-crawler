package pl.ekodo.crawler.focused.engine

import java.net.URI

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import kamon.Kamon

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object Engine extends App {

  ConfigParser.parser.parse(args, RuntimeConfig()) match {
    case Some(config) =>
      Kamon.start()
      val system: ActorSystem = ActorSystem("focused-crawler")
      val supervisor = system.actorOf(Supervisor.props(config), "supervisor")

      implicit val timeout = Timeout(5.seconds)
      implicit val ec: ExecutionContext = system.dispatcher

      system.scheduler.schedule(5.seconds, 5.seconds) {
        val response = (supervisor ? Supervisor.GetAppStatus).mapTo[Supervisor.AppStatus]
        response.onComplete {
          case Success(r) if r == Supervisor.Finished =>
            system.log.info("Finished job")
            system.terminate()
            Kamon.shutdown()
          case Success(r) if r == Supervisor.Working =>
            system.log.debug("Crawler still working")
          case Failure(ex) =>
            system.log.warning("Could not get app status")
        }
      }
    case None =>
      sys.exit(-1)
  }

}

case class RuntimeConfig(
  seeds: Seq[URI] = Seq(),
  depth: Int = 5,
  maxLinksNumber: Int = 10000,
  topLevelDomainPolicy: String = "",
  containsPolicy: String = "",
  regexpPolicy: String = ""
)

object ConfigParser {

  val parser = new scopt.OptionParser[RuntimeConfig]("Focused Crawler") {
    head("Focused Crawler", "0.0.1-SNAPSHOT")

    opt[Int]('d', "depth").required()
      .action((d, c) => c.copy(depth = d)).text("Depth of search")
      .validate(d =>
        if (d > 0) success
        else failure("Must be positive")
      )

    opt[Seq[URI]]('s', "seeds").required()
      .valueName("<http://seed1>,<https://seed2>...")
      .action((seeds, c) => c.copy(seeds = seeds)).text("Link seeds")
      .validate(seeds =>
        if (seeds.forall(s => s.getScheme == "http" || s.getScheme == "https")) success
        else failure("Each seed link must starts with http:// or https://")
      )

    opt[Int]('l', "max-links").required()
      .action((l, c) => c.copy(maxLinksNumber = l)).text("Maximum number of links")
      .validate(l =>
        if (l > 0 && l < 10000) success
        else failure("Must be positive and less than 10000")
      )

    opt[String]("tld").action((x, c) =>
      c.copy(topLevelDomainPolicy = x)).text("Search links from given top level domain")

    opt[String]("contains").action((x, c) =>
      c.copy(containsPolicy = x)).text("Search links with given text")

    opt[String]("regexp").action((x, c) =>
      c.copy(regexpPolicy = x)).text("Search links which match regexp")
      .validate { regexp =>
        val r = Try(regexp.r)
        r.fold(
          exp => failure(exp.getMessage),
          r => success
        )
      }

  }
}
