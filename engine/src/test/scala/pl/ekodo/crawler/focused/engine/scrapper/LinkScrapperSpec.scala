package pl.ekodo.crawler.focused.engine.scrapper

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import pl.ekodo.crawler.focused.engine.AkkaTest

import scala.util.{Success, Try}

class LinkScrapperSpec extends AkkaTest(ActorSystem("link-scrapper-spec")) {

  "LinkScrapper" should "pass found links from given url" in {
    val sender = TestProbe()
    val linkScrapper = system.actorOf(LinkScrapper.props(TestScrapper))
    val request = LinkScrapper.GetLinks(new URL("http://example.com"), new URL("http://example.com"), 0)
    linkScrapper.tell(request, sender.ref)
    sender.expectMsg(
      LinkScrapper.GetLinksOK(
        request,
        Set(
          Link(new URL("http://example.com/one"), "<a href='http://example.com/one'>One</a>"),
          Link(new URL("http://example.com/two"), "<a href='http://example.com/two'>Two</a>"),
          Link(new URL("http://example.com/three"), "<a href='http://example.com/three'>Three</a>")
        )
      )
    )
  }

  it should "pass empty set of links if scrapper did not find links" in {
    val sender = TestProbe()
    val linkScrapper = system.actorOf(LinkScrapper.props(TestScrapper))
    val request = LinkScrapper.GetLinks(new URL("http://empty.com"), new URL("http://empty.com"), 0)
    linkScrapper.tell(request, sender.ref)
    sender.expectMsg(
      LinkScrapper.GetLinksOK(
        request,
        Set.empty
      )
    )
  }

}

object TestScrapper extends Scrapper {

  private val Example = new URL("http://example.com")

  private val Empty = new URL("http://empty.com")

  override def links(url: URL): Try[Set[Link]] = url match {
    case Example =>
      Success(
        Set(
          Link(new URL("http://example.com/one"), "<a href='http://example.com/one'>One</a>"),
          Link(new URL("http://example.com/two"), "<a href='http://example.com/two'>Two</a>"),
          Link(new URL("http://example.com/three"), "<a href='http://example.com/three'>Three</a>")
        )
      )
    case Empty =>
      Success(
        Set.empty
      )
  }

}
