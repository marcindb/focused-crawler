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

    val indexer = system.actorOf(Indexer.props(3, Set(url1, url2, url3), scheduler.ref))

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

    val indexer = TestActorRef(Indexer.props(3, Set(url1, url2, url3), scheduler.ref))

    indexer.children.size shouldEqual 3
  }

  it should "not send already sent link to scheduler" in {
    val url = new URL("http://google.com")

    val scheduler = TestProbe()

    val indexer = system.actorOf(Indexer.props(3, Set(url), scheduler.ref))

    scheduler.expectMsgAllOf(
      Scheduler.Register(indexer),
      Scheduler.Schedule(url, 0, Set(url))
    )

    indexer ! Indexer.Index(url, url, 1, Set(Link(url, "")))

    scheduler.expectNoMsg()
  }

  it should "send new urls to scheduler with increased depth" in {
    val url = new URL("http://google.com")

    val scheduler = TestProbe()

    val indexer = system.actorOf(Indexer.props(3, Set(url), scheduler.ref))

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

    val indexer = system.actorOf(Indexer.props(3, Set(url), scheduler.ref))

    scheduler.expectMsgAllOf(
      Scheduler.Register(indexer),
      Scheduler.Schedule(url, 0, Set(url))
    )

    val foundURL = new URL("http://google.com/policies")
    indexer ! Indexer.Index(url, url, 3, Set(Link(foundURL, "")))

    scheduler.expectNoMsg()
  }

}
