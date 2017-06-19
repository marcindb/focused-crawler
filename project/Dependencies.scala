import sbt._

object Dependencies {

  private val akkaVersion = "2.5.2"

  private val kamonVersion = "0.6.7"

  private val graphVersion = "1.11.5"

  val akka = {
    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  }

  val scrapper = {
    Seq(
      "net.ruippeixotog" %% "scala-scraper" % "2.0.0-RC2"
    )
  }

  val scopt = {
    Seq(
      "com.github.scopt" %% "scopt" % "3.6.0"
    )
  }

  val kamon = {
    Seq(
      "io.kamon" %% "kamon-core" % kamonVersion,
      "io.kamon" %% "kamon-akka-2.5" % kamonVersion,
      "io.kamon" %% "kamon-log-reporter" % kamonVersion,
      "io.kamon" %% "kamon-statsd" % kamonVersion,
      "io.kamon" %% "kamon-system-metrics" % kamonVersion,
      // loader which loads Sigar JAR to get system metrics
      "io.kamon" % "sigar-loader" % "1.6.6-rev002",
      "org.aspectj" % "aspectjweaver" % "1.8.10"
    )
  }

  val graph = {
    Seq(
      "org.scala-graph" %% "graph-core" % graphVersion,
      "org.scala-graph" %% "graph-dot" % graphVersion
    )
  }

}