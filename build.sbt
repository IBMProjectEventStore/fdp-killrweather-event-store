import Dependencies._

autoScalaLibrary := false

libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value % "provided"

lazy val protobufs = (project in file("./protobufs"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ))

lazy val killrWeatherCore = (project in file("./killrweather-core"))
  .settings(defaultSettings:_*)
  .settings(libraryDependencies ++= core)


lazy val killrWeatherApp = (project in file("./killrweather-app"))
  .settings(defaultSettings:_*)
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7")
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .settings(dependencyDotFile := file("dependencies.dot"))
  .dependsOn(killrWeatherCore, protobufs)


lazy val loader = (project in file("./killrweather-loader"))
  .settings(defaultSettings:_*)
  .dependsOn(killrWeatherCore, protobufs)

lazy val dailyreader = (project in file("./killrweather-daylyreader"))
  .settings(defaultSettings:_*)
  .dependsOn(killrWeatherCore, protobufs)

lazy val killrweather = (project in file("."))
  .aggregate(killrWeatherCore, killrWeatherApp,loader, dailyreader, protobufs)

