lazy val root = (project in file(".")).
  settings(
    name := "sudoku",
    version := "1.0",
    scalaVersion := "2.11.7"
  )

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.0" % "test"
libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.2"
coverageEnabled := true


