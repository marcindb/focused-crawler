package pl.ekodo.crawler.focused.engine

import java.net.URI

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import pl.ekodo.crawler.focused.engine.Supervisor.GetResults

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object Engine extends App {

  ConfigParser.parser.parse(args, RuntimeConfig()) match {
    case Some(config) =>
      val system: ActorSystem = ActorSystem("focused-crawler")
      val root = system.actorOf(Supervisor.props(config), "supervisor")

      implicit val timeout = Timeout(60.seconds)
      implicit val ec: ExecutionContext = system.dispatcher

      val response = (root ? GetResults)
      response.onComplete {
        case Success(r) =>
          system.log.info(s"$r")
          system.terminate()
        case Failure(ex) =>
          system.terminate()
      }
    case None =>
      sys.exit(-1)
  }

}

case class RuntimeConfig(seeds: Seq[URI] = Seq(), depth: Int = 5, maxLinksNumber: Int = 10000)

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

  }
}
