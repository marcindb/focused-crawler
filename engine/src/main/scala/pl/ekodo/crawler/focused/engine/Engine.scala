package pl.ekodo.crawler.focused.engine

import akka.actor.ActorSystem

object Engine extends App {

  val system: ActorSystem = ActorSystem("focused-crawler")

  system.terminate()

}
