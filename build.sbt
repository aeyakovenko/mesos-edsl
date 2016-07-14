lazy val root = (project in file(".")).
  settings(
    name := "mesos-edsl",
    version := "1.0",
    scalaVersion := "2.11.8"
  )

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.0" % "test"
libraryDependencies += "org.typelevel" %% "cats" % "0.6.0"
libraryDependencies += "org.apache.mesos" % "mesos" % "0.28.1"

coverageEnabled in Test := true
