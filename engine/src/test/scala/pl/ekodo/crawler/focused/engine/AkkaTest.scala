package pl.ekodo.crawler.focused.engine

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

class AkkaTest(_system: ActorSystem) extends TestKit(_system)
                                             with Matchers
                                             with FlatSpecLike
                                             with BeforeAndAfterAll {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

}
