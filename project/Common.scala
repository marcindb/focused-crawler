import sbt.Keys._
import sbt._

object Common {

  val settings: Seq[Setting[_]] = Seq(
    organization := "pl.ekodo",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.2",
    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")
  )

}