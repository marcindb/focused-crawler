package pl.ekodo.crawler.focused.engine.frontier

import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import akka.actor.{Actor, ActorLogging, Props}
import pl.ekodo.crawler.focused.engine.frontier.SeedIndexer.{Connection, GenerateGraph, GenerateGraphOK, IndexConnections}

import scala.language.{existentials, implicitConversions}
import scalax.collection.Graph
import scalax.collection.GraphEdge._
import scalax.collection.GraphPredef._
import scalax.collection.io.dot._
import scalax.collection.io.dot.{DotEdgeStmt, DotGraph, DotRootGraph, Id, NodeId}


object SeedIndexer {

  case class Connection(from: URL, to: URL)

  case class IndexConnections(connections: Set[Connection])

  case class GenerateGraph(output: String)

  case class GenerateGraphOK(file: Path)

  case class GenerateGraphError(exp: Throwable)

  def props(seed: URL) = Props(new SeedIndexer(seed))

}

class SeedIndexer(seed: URL) extends Actor with ActorLogging {

  var links = Set.empty[Connection]

  override def receive: Receive = {
    case IndexConnections(conns) =>
      links = links ++ conns
    case GenerateGraph(output) =>
      val filename = seed.getHost.replace('.','_') + ".dot"
      val outputFile = Paths.get(output, filename)
      Files.write(outputFile, graph(links).getBytes(StandardCharsets.UTF_8))
      log.info("Graph generated for seed: {}, output: {}", seed, outputFile)
      sender ! GenerateGraphOK(outputFile)
  }


  def graph(connections: Set[Connection]): String = {
    val graphConnections = connections.map(c => c.from.toString ~> c.to.toString).toSeq
    val g = Graph[String, DiEdge](graphConnections: _*)
    val root = DotRootGraph(directed = true, id = Some(Id(seed.toString)))

    def edgeTransformer(innerEdge: Graph[String, DiEdge]#EdgeT): Option[(DotGraph, DotEdgeStmt)] = {
      val edge = innerEdge.edge
      Some(root,
        DotEdgeStmt(NodeId(edge.from.toString),
          NodeId(edge.to.toString), Nil))
    }

    graph2DotExport(g).toDot(dotRoot = root, edgeTransformer = edgeTransformer)
  }

}
