package pl.ekodo.crawler.focused.engine.frontier
import pl.ekodo.crawler.focused.engine.scrapper.Link

import scala.util.matching.Regex

object Policy {

  /**
    * Combines policies and returns combined policy
    *
    * @param policies policies to combine
    * @return         combined policy
    */
  def apply(policies: Indexer.Policy*): Indexer.Policy = {
    link => policies.foldLeft(true)((acc, p) => if(acc) p(link) else false)
  }

}

/**
  * Policy which always passes
  */
object AlwaysPass extends Indexer.Policy {
  override def apply(link: Link): Boolean = true
}

/**
  * Policy which passes links from given TLD
  *
  * @param tld top level domain (TLD)
  */
class TopLevelDomain(tld: String) extends Indexer.Policy {

  override def apply(link: Link): Boolean = {
    val parts = link.url.getHost.split("\\.")
    if(parts.size > 1) {
      parts.last == tld
    } else false
  }

}

/**
  * Policy which passes links consisting given pattern in html anchor
  *
  * @param pattern pattern
  */
class HtmlContains(pattern: String) extends Indexer.Policy {

  override def apply(link: Link): Boolean = link.html.contains(pattern)
}

/**
  * Policy which passes link if html anchor satisfies given regex
  *
  * @param regex regex
  */
class HtmlRegexp(regex: Regex) extends Indexer.Policy {

  override def apply(link: Link): Boolean = regex.findFirstIn(link.html).isDefined

}
