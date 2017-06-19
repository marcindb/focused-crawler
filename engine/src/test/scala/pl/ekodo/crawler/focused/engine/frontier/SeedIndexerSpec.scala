package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL
import java.nio.file.Files

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import pl.ekodo.crawler.focused.engine.AkkaTest

import scala.concurrent.duration._

class SeedIndexerSpec extends AkkaTest(ActorSystem("scheduler-spec")) {

  "SeedIndexer" should "generate graph" in {
    val testProbe = TestProbe()
    val indexer = system.actorOf(SeedIndexer.props(new URL("http://bbc.com")))
    indexer ! SeedIndexer.IndexConnections(
      Set(
        SeedIndexer.Connection(new URL("http://bbc.com"), new URL("http://bbc.com/news")),
          SeedIndexer.Connection(new URL("http://bbc.com/news"), new URL("http://bbc.com/news/poland")),
          SeedIndexer.Connection(new URL("http://bbc.com/news"), new URL("http://bbc.com/news/france"))
      )
    )

    val output = Files.createTempDirectory("graphs")

    indexer.tell(SeedIndexer.GenerateGraph(output.toString), testProbe.ref)

    val expectedLinks = Set(
      "\"http://bbc.com\" -> \"http://bbc.com/news\"",
      "\"http://bbc.com/news\" -> \"http://bbc.com/news/poland\"",
      "\"http://bbc.com/news\" -> \"http://bbc.com/news/france\""
    )

    testProbe.expectMsgPF(3.seconds) {
      case SeedIndexer.GenerateGraphOK(path) =>
        val graph = new String(Files.readAllBytes(path))
        expectedLinks.foreach { link =>
          graph.contains(link) shouldBe true
        }

    }

  }

}
