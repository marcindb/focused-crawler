package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{TestActorRef, TestProbe}
import pl.ekodo.crawler.focused.engine.AkkaTest
import pl.ekodo.crawler.focused.engine.scrapper.LinkScrapper.{GetLinks, GetLinksOK}
import pl.ekodo.crawler.focused.engine.scrapper.SiteScrapper

import scala.concurrent.duration._

class SchedulerSpec extends AkkaTest(ActorSystem("scheduler-spec")) {

  "Scheduler" should "register indexer on startup" in {
    val testProbe = TestProbe()
    val called = new AtomicBoolean(false)
    val scrapperProps = Props(new Actor {
      override def receive: Receive = {
        case gl: GetLinks => called.set(true)
      }
    })
    val scheduler = system.actorOf(Scheduler.props(scrapperProps))
    val url = new URL("http://google.com")

    scheduler.tell(Scheduler.Register(testProbe.ref), testProbe.ref)
    scheduler.tell(Scheduler.Schedule(url, 0, Set(url)), testProbe.ref)

    awaitCond(called.get, 3.seconds)
  }

  it should "not accept any messages if indexer is not registered" in {
    val testProbe = TestProbe()
    val called = new AtomicBoolean(false)
    val scrapperProps = Props(new Actor {
      override def receive: Receive = {
        case gl: GetLinks => called.set(true)
      }
    })
    val scheduler = system.actorOf(Scheduler.props(scrapperProps))
    val url = new URL("http://google.com")

    scheduler.tell(Scheduler.Schedule(url, 0, Set(url)), testProbe.ref)

    called.get shouldBe false
  }

  it should "schedule processing of all passed urls in Schedule message" in {
    val testProbe = TestProbe()
    val indexer = TestProbe()

    val scrapperProps = Props(new Actor {
      override def receive: Receive = {
        case gl: GetLinks =>
          sender ! GetLinksOK(gl, Set.empty)
      }
    })
    val scheduler = system.actorOf(Scheduler.props(scrapperProps))
    val seed = new URL("http://google.com")
    val urls = Set(new URL("http://google.com/policies"), new URL("http://google.com/about"))

    scheduler.tell(Scheduler.Register(indexer.ref), testProbe.ref)
    scheduler.tell(Scheduler.Schedule(seed, 0, urls), testProbe.ref)

    indexer.expectMsgAllOf(
      Indexer.Index(new URL("http://google.com/policies"), seed, 0, Set.empty),
      Indexer.Index(new URL("http://google.com/about"), seed, 0, Set.empty)
    )

  }

  it should "handle MaxRequestsExceeded message from site scrapper and do not process messages from this domain" in {
    val testProbe = TestProbe()
    val indexer = TestProbe()

    val scrapperProps = Props(new Actor {
      override def receive: Receive = {
        case gl: GetLinks =>
          sender ! GetLinksOK(gl, Set.empty)
      }
    })
    val scheduler = TestActorRef(Scheduler.props(scrapperProps))
    val seed = new URL("http://google.com")
    val urls = Set(new URL("http://google.com/policies"), new URL("http://google.com/about"))

    scheduler.tell(Scheduler.Register(indexer.ref), testProbe.ref)
    scheduler.tell(Scheduler.Schedule(seed, 0, urls), testProbe.ref)

    indexer.expectMsgAllOf(
      Indexer.Index(new URL("http://google.com/policies"), seed, 0, Set.empty),
      Indexer.Index(new URL("http://google.com/about"), seed, 0, Set.empty)
    )

    scheduler.children.filterNot(_.path.name == "scrappers").foreach { siteScrapper =>
      scheduler.tell(SiteScrapper.MaxRequestsExceeded, siteScrapper)
    }

    scheduler.tell(Scheduler.Schedule(seed, 1, urls), testProbe.ref)

    indexer.expectNoMsg()

  }

  it should "handle NoMoreWork message from site scrapper" in {
    val testProbe = TestProbe()
    val indexer = TestProbe()

    val scrapperProps = Props(new Actor {
      override def receive: Receive = {
        case gl: GetLinks =>
          sender ! GetLinksOK(gl, Set.empty)
      }
    })
    val scheduler = TestActorRef(Scheduler.props(scrapperProps))
    val seed = new URL("http://google.com")
    val urls = Set(new URL("http://google.com/policies"), new URL("http://google.com/about"))

    scheduler.tell(Scheduler.Register(indexer.ref), testProbe.ref)
    scheduler.tell(Scheduler.Schedule(seed, 0, urls), testProbe.ref)

    indexer.expectMsgAllOf(
      Indexer.Index(new URL("http://google.com/policies"), seed, 0, Set.empty),
      Indexer.Index(new URL("http://google.com/about"), seed, 0, Set.empty)
    )

    scheduler.children.filterNot(_.path.name == "scrappers").foreach { siteScrapper =>
      scheduler.tell(SiteScrapper.NoMoreWork, siteScrapper)
    }

    scheduler.tell(Scheduler.Schedule(seed, 1, urls), testProbe.ref)

    indexer.expectNoMsg()
  }

}
