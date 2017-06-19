package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestProbe}
import pl.ekodo.crawler.focused.engine.AkkaTest
import pl.ekodo.crawler.focused.engine.scrapper.Link

class IndexerSpec extends AkkaTest(ActorSystem("indexer-spec")) {

  "Indexer" should "send Schedule message for each seed on startup" in {
    val url1 = new URL("http://google.com")
    val url2 = new URL("http://apple.com")
    val url3 = new URL("http://tesla.com")

    val scheduler = TestProbe()

    val indexer = system.actorOf(Indexer.props("", 3, Set(url1, url2, url3), scheduler.ref, AlwaysPass))

    scheduler.expectMsg(
      Scheduler.Register(indexer)
    )

    scheduler.expectMsgAllOf(
      Scheduler.Schedule(url1, 0, Set(url1)),
      Scheduler.Schedule(url2, 0, Set(url2)),
      Scheduler.Schedule(url3, 0, Set(url3))
    )
  }

  it should "create SeedIndexer for each seed" in {
    val url1 = new URL("http://google.com")
    val url2 = new URL("http://apple.com")
    val url3 = new URL("http://tesla.com")

    val scheduler = TestProbe()

    val indexer = TestActorRef(Indexer.props("", 3, Set(url1, url2, url3), scheduler.ref, AlwaysPass))

    indexer.children.size shouldEqual 3
  }

  it should "not send already sent link to scheduler" in {
    val url = new URL("http://google.com")

    val scheduler = TestProbe()

    val indexer = system.actorOf(Indexer.props("", 3, Set(url), scheduler.ref, AlwaysPass))

    scheduler.expectMsgAllOf(
      Scheduler.Register(indexer),
      Scheduler.Schedule(url, 0, Set(url))
    )

    indexer ! Indexer.Index(url, url, 1, Set(Link(url, "")))

    scheduler.expectNoMsg()
  }

  it should "not send link which does not pass policy" in {
    val seed = new URL("http://bbc.com")

    val scheduler = TestProbe()

    val tld = "com"
    val tldPolicy = new TopLevelDomain(tld)
    val date = """\d\d\d\d-\d\d-\d\d""".r
    val regexpPolicy = new HtmlRegexp(date)
    val policy = Policy(tldPolicy, regexpPolicy)

    val indexer = system.actorOf(Indexer.props("", 3, Set(seed), scheduler.ref, policy))

    scheduler.expectMsgAllOf(
      Scheduler.Register(indexer),
      Scheduler.Schedule(seed, 0, Set(seed))
    )

    val validLink = Link(new URL("http://bbc.com/poland"), "<a href='http://bbc.com/poland'>Poland News 2017-06-19</a>")
    val invalidLink = Link(new URL("http://bbc.co.uk"), "<a href='http://bbc.com/poland'>Poland News 2017-06-19</a>")

    indexer ! Indexer.Index(seed, seed, 0, Set(validLink, invalidLink))

    scheduler.expectMsg(
      Scheduler.Schedule(seed, 1, Set(validLink.url))
    )
  }

  it should "send new urls to scheduler with increased depth" in {
    val url = new URL("http://google.com")

    val scheduler = TestProbe()

    val indexer = system.actorOf(Indexer.props("", 3, Set(url), scheduler.ref, AlwaysPass))

    scheduler.expectMsgAllOf(
      Scheduler.Register(indexer),
      Scheduler.Schedule(url, 0, Set(url))
    )

    val foundURL = new URL("http://google.com/policies")
    indexer ! Indexer.Index(url, url, 0, Set(Link(foundURL, "")))

    scheduler.expectMsg(
      Scheduler.Schedule(url, 1, Set(foundURL))
    )
  }

  it should "not send urls if depth is equal to maximum depth passed to Indexer" in {
    val url = new URL("http://google.com")

    val scheduler = TestProbe()

    val indexer = system.actorOf(Indexer.props("", 3, Set(url), scheduler.ref, AlwaysPass))

    scheduler.expectMsgAllOf(
      Scheduler.Register(indexer),
      Scheduler.Schedule(url, 0, Set(url))
    )

    val foundURL = new URL("http://google.com/policies")
    indexer ! Indexer.Index(url, url, 3, Set(Link(foundURL, "")))

    scheduler.expectNoMsg()
  }

}
