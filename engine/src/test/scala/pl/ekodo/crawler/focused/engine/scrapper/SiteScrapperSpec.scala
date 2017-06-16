package pl.ekodo.crawler.focused.engine.scrapper

import java.net.URL

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestProbe
import org.scalatest.concurrent.ScalaFutures
import pl.ekodo.crawler.focused.engine.AkkaTest
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper.GetLinks


class SiteScrapperSpec extends AkkaTest(ActorSystem("scrapper-spec")) with ScalaFutures {

  val config: SiteScrapperConfig = SiteScrapperConfig("example.com", 5)

  "SiteScrapper" should "monitor if site is not trap" in {
    val probe = TestProbe()
    val indexer = TestProbe()
    val siteScrapper = system.actorOf(SiteScrapper.props(config, indexer.ref, system.actorOf(Props[TestLinkScrapper])))
    for (i <- 1 to 6) {
      siteScrapper.tell(SiteScrapper.Process(new URL("http://example.com"), new URL("http://example.com"), 0), probe.ref)
    }
    probe.expectMsg(SiteScrapper.MaxRequestsExceeded)
  }

}

class TestLinkScrapper extends Actor {
  override def receive: Receive = {
    case gl: GetLinks =>
      sender ! LinkScrapper.GetLinksOK(gl, Set.empty)
  }
}
