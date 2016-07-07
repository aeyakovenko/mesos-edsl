lazy val root = (project in file(".")).
  settings(
    name := "mesos-edsl",
    version := "1.0",
    scalaVersion := "2.11.7"
  )

libraryDependencies += "org.scalacheck" %% "scalacheck" % "1.13.0" % "test"
unmanagedBase <<= baseDirectory { base => base / "mesos-0.28.2" }

coverageEnabled := true
