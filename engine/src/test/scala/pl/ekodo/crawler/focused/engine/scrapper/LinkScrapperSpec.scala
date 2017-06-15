package pl.ekodo.crawler.focused.engine.scrapper

import akka.actor.ActorSystem
import akka.testkit.TestProbe
import pl.ekodo.crawler.focused.engine.AkkaTest

class LinkScrapperSpec extends AkkaTest(ActorSystem("link-scrapper-spec")) {

  "LinkScrapper" should "pass found links from given url" in {
    val indexer = TestProbe()
    val linkScrapper = system.actorOf(LinkScrapper.props(indexer.ref, TestScrapper))
    linkScrapper ! LinkScrapper.GetLinks("http://example.com")
    indexer.expectMsg(
      LinkScrapper.ScrappedLinks(
        "http://example.com",
        Set(
          Link("http://example.com/one", "<a href='http://example.com/one'>One</a>"),
          Link("http://example.com/two", "<a href='http://example.com/two'>Two</a>"),
          Link("http://example.com/three", "<a href='http://example.com/three'>Three</a>")
        )
      )
    )
  }

  it should "pass empty set of links if scrapper did not find links" in {
    val indexer = TestProbe()
    val linkScrapper = system.actorOf(LinkScrapper.props(indexer.ref, TestScrapper))
    linkScrapper ! LinkScrapper.GetLinks("http://empty.com")
    indexer.expectMsg(
      LinkScrapper.ScrappedLinks(
        "http://empty.com",
        Set.empty
      )
    )
  }

}

object TestScrapper extends Scrapper {

  private val Example = "http://example.com"

  private val Empty = "http://empty.com"

  override def links(url: String): Set[Link] = url match {
    case Example =>
      Set(
        Link("http://example.com/one", "<a href='http://example.com/one'>One</a>"),
        Link("http://example.com/two", "<a href='http://example.com/two'>Two</a>"),
        Link("http://example.com/three", "<a href='http://example.com/three'>Three</a>")
      )
    case Empty =>
      Set.empty
  }

}
