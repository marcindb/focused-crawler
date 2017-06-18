import com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._
import kamon.aspectj.sbt.AspectjRunner
import sbt.Keys._
import sbt._

object Common {

  val settings: Seq[Setting[_]] = Seq(
    organization := "pl.ekodo",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.2",
    fork in run := true,
    scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature", "-target:jvm-1.8")
  )

  /**
    * These are Kamon specific settings for adding weaver for instrumentation
    */
  val kamonInstrumentationSettings: Seq[Setting[_]] = Seq(
    bashScriptExtraDefines ++= Seq(
      """addJava "-javaagent:${app_home}/../lib/org.aspectj.aspectjweaver-1.8.10.jar"""",
      """addJava "-javaagent:${app_home}/../lib/io.kamon.sigar-loader-1.6.6-rev002.jar""""
    ),
    (run in Compile) := (run in AspectjRunner.Runner).evaluated
  )

}