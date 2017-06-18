/*
 * Aggregates and builds top level components
 */
scalaVersion := "2.12.2"

lazy val root = Project("focused-crawler", file(".")).
  aggregate(
    engine
  )

lazy val engine = project.in(file("engine")).
  settings(Common.settings: _*).
  settings(Common.kamonInstrumentationSettings: _*).
  settings(
    libraryDependencies ++= Dependencies.akka
      ++ Dependencies.scrapper
      ++ Dependencies.scopt
      ++ Dependencies.kamon
  ).enablePlugins(JavaAppPackaging)
