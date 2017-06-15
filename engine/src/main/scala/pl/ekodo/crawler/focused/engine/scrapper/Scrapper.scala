package pl.ekodo.crawler.focused.engine.scrapper

import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.{Document, Element}

case class Link(href: String, html: String)

trait Scrapper {

  def links(url: String): Set[Link]

}

object JsoupScrapper extends Scrapper {

  private val Anchor = "a"

  private val Href = "href"

  private val Http = "http"

  override def links(url: String): Set[Link] = links(JsoupBrowser().get(url))

  def links(doc: Document) = {
    val allLinks = doc >> elementList (Anchor)
    allLinks.flatMap(toLink).toSet
  }

  private def isAbsolute(el: Element): Boolean = el.hasAttr(Href) && el.attr(Href).startsWith(Http)

  private def toLink(el: Element): Option[Link] =
    if(isAbsolute(el)) Some(Link(el.attr(Href), el.outerHtml)) else None

}
