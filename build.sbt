/*
 * Aggregates and builds top level components
 */
lazy val root = Project("focused-crawler", file(".")).
  aggregate(
    engine
  )

lazy val engine = project.in(file("engine")).
  settings(Common.settings: _*).
  settings(
    libraryDependencies ++= Dependencies.akka
      ++ Dependencies.scrapper
      ++ Dependencies.scopt
  )
