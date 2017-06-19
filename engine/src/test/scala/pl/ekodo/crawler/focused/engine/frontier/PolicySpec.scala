package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import org.scalatest.{FlatSpec, Matchers}
import pl.ekodo.crawler.focused.engine.scrapper.Link

class PolicySpec extends FlatSpec with Matchers {

  "TopLevelDomain" should "pass links with given top level domain" in {
    val tld = "pl"
    val policy = new TopLevelDomain(tld)
    val plLink = Link(new URL("http://google.pl"), "")
    val comLink = Link(new URL("http://google.com"), "")

    policy(plLink) shouldBe true
    policy(comLink) shouldBe false
  }

  "HtmlContains" should "pass links with given string in link html" in {
    val pattern = "Poland"
    val policy = new HtmlContains(pattern)
    val validLink = Link(new URL("http://bbc.com"), "<a href='http://bbc.com/poland'>Poland News</a>")
    val invalidLink = Link(new URL("http://bbc.com"), "<a href='http://bbc.com/news'>News</a>")

    policy(validLink) shouldBe true
    policy(invalidLink) shouldBe false
  }

  "HtmlRegexp" should "pass links with given regexp in link html" in {
    val date = """\d\d\d\d-\d\d-\d\d""".r
    val policy = new HtmlRegexp(date)
    val validLink = Link(new URL("http://bbc.com"), "<a href='http://bbc.com/poland'>Poland News 2017-06-19</a>")
    val invalidLink = Link(new URL("http://bbc.com"), "<a href='http://bbc.com/news'>News</a>")

    policy(validLink) shouldBe true
    policy(invalidLink) shouldBe false
  }


  "Policy" should "combine policies" in {
    val tld = "com"
    val tldPolicy = new TopLevelDomain(tld)
    val date = """\d\d\d\d-\d\d-\d\d""".r
    val regexpPolicy = new HtmlRegexp(date)
    val policy = Policy(tldPolicy, regexpPolicy)

    val validLink = Link(new URL("http://bbc.com"), "<a href='http://bbc.com/poland'>Poland News 2017-06-19</a>")
    val invalidLink = Link(new URL("http://bbc.co.uk"), "<a href='http://bbc.com/poland'>Poland News 2017-06-19</a>")

    policy(validLink) shouldBe true
    policy(invalidLink) shouldBe false
  }

  it should "return always true if no policy is defined" in {
    val policy = Policy(List.empty: _*)
    val link = Link(new URL("http://bbc.com"), "<a href='http://bbc.com/news'>News</a>")
    policy(link) shouldBe true
  }

}
