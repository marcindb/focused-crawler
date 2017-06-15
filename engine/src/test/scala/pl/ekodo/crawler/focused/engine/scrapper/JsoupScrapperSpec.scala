package pl.ekodo.crawler.focused.engine.scrapper

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import org.scalatest.{FlatSpec, Matchers}

class JsoupScrapperSpec extends FlatSpec with Matchers {

  "JsoupScrapper" should "parse only absolute urls" in {
    val example = getClass.getResourceAsStream("example.html")
    val doc = JsoupBrowser().parseInputStream(example)
    val links = JsoupScrapper.links(doc)

    links should contain only (
      Link("http://example.com", "<a href=\"http://example.com\">Example</a>"),
      Link("http://example.com/one", "<a href=\"http://example.com/one\">One</a>")
      )
  }

  it should "return empty set if did not find links" in {
    val example = getClass.getResourceAsStream("empty.html")
    val doc = JsoupBrowser().parseInputStream(example)
    val links = JsoupScrapper.links(doc)

    links shouldBe empty
  }

}
