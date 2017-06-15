package pl.ekodo.crawler.focused.engine

import java.net.URI

import akka.actor.ActorSystem

object Engine extends App {

  ConfigParser.parser.parse(args, Config()) match {
    case Some(config) =>
      val system: ActorSystem = ActorSystem("focused-crawler")
      system.terminate()

    case None =>
      sys.exit(-1)
  }

}

case class Config(seeds: Seq[URI] = Seq(), depth: Int = 5)

object ConfigParser {

  val parser = new scopt.OptionParser[Config]("Focused Crawler") {
    head("Focused Crawler", "0.0.1-SNAPSHOT")

    opt[Int]('d', "depth").required()
      .action((d, c) => c.copy(depth = d)).text("Depth of search")
      .validate(d =>
        if(d >0) success
        else failure("Must be positive")
      )

    opt[Seq[URI]]('s', "seeds").required()
      .valueName("<http://seed1>,<https://seed2>...")
      .action((seeds, c) => c.copy(seeds = seeds)).text("Link seeds")
      .validate(seeds =>
        if(seeds.forall(s => s.getScheme == "http" || s.getScheme == "https")) success
        else failure("Each seed link must starts with http:// or https://")
      )

  }
}
