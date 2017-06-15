package pl.ekodo.crawler.focused.engine.scrapper

import akka.actor.{Actor, ActorSystem, Props}
import org.scalatest.concurrent.ScalaFutures
import pl.ekodo.crawler.focused.engine.AkkaTest
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper.GetLinks
import pl.ekodo.crawler.focused.engine.scrapper.SiteScrapper.MaxLinksExceeded

import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Success

class SiteScrapperSpec extends AkkaTest(ActorSystem("scrapper-spec")) with ScalaFutures {

  val config: SiteScrapperConfig = SiteScrapperConfig("example.com", "example.com", 1.millis, 5)

  "SiteScrapper" should "send GetLinks message with seed to LinkScrapper" in {
    val p = Promise[GetLinks]()
    val linkScrapper = Props(classOf[TestLinkScrapper], p)
    system.actorOf(SiteScrapper.props(config, linkScrapper))
    p.future.isReadyWithin(100.millis) shouldBe true
  }

  it should "monitor if site is not trap" in {
    val siteScrapper = system.actorOf(SiteScrapper.props(config, Props.empty))
    for(i <- 1 to 5) {
      siteScrapper.tell(SiteScrapper.Process("http://example.com"), testActor)
    }
    expectMsg(SiteScrapper.Done(MaxLinksExceeded))
  }

}

class TestLinkScrapper(promise: Promise[GetLinks]) extends Actor {
  override def receive: Receive = {
    case gl: GetLinks =>
      promise.complete(Success(gl))
  }
}