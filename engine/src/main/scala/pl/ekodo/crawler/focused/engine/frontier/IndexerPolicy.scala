package pl.ekodo.crawler.focused.engine.frontier
import pl.ekodo.crawler.focused.engine.scrapper.Link

import scala.util.matching.Regex

object Policy {

  def apply(policies: Indexer.Policy*): Indexer.Policy = {
    link => policies.foldLeft(true)((acc, p) => if(acc) p(link) else false)
  }

}

object AlwaysPass extends Indexer.Policy {
  override def apply(link: Link): Boolean = true
}


class TopLevelDomain(tld: String) extends Indexer.Policy {

  override def apply(link: Link): Boolean = {
    val parts = link.url.getHost.split("\\.")
    if(parts.size > 1) {
      parts.last == tld
    } else false
  }

}

class HtmlContains(pattern: String) extends Indexer.Policy {

  override def apply(link: Link): Boolean = link.html.contains(pattern)
}

class HtmlRegexp(regex: Regex) extends Indexer.Policy {

  override def apply(link: Link): Boolean = regex.findFirstIn(link.html).isDefined

}
