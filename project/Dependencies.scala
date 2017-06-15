import sbt._

object Dependencies {

  private val akkaVersion = "2.5.2"

  val akka = {
    Seq(
      "com.typesafe.akka" %% "akka-actor" % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
      "org.scalatest" %% "scalatest" % "3.0.1" % "test"
    )
  }

}