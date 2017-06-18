package pl.ekodo.crawler.focused.engine.scrapper

import java.net.URL

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Document, Element}

import scala.util.Try

case class Link(url: URL, html: String)

trait Scrapper {

  def links(url: URL): Try[Set[Link]]

}

object JsoupScrapper extends Scrapper {

  private val Anchor = "a"

  private val Href = "href"

  private val Http = "http"

  override def links(url: URL): Try[Set[Link]] =
    Try(links(JsoupBrowser().get(url.toString)))

  def links(doc: Document) = (doc >> elementList(Anchor)).flatMap(toLink).toSet

  private def isAbsolute(el: Element): Boolean = el.hasAttr(Href) && el.attr(Href).startsWith(Http)

  private def toLink(el: Element): Option[Link] =
    if (isAbsolute(el))
      Try(new URL(el.attr(Href))).map(url => Link(url, el.outerHtml)).toOption
    else
      None

}
