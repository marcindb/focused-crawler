package pl.ekodo.crawler.focused.engine.frontier

import akka.actor.ActorSystem
import pl.ekodo.crawler.focused.engine.AkkaTest

class SeedIndexerSpec extends AkkaTest(ActorSystem("scheduler-spec")) {

  "SeedIndexer" should "" in {
    println(SeedIndexer.graph())
  }

}
