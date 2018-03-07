import Dependencies._
import deployssh.DeploySSH._

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
  .settings(
    mainClass in Compile := Some("com.lightbend.killrweather.app.KillrWeatherEventStore"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather Spark Runner",
    packageDescription := "KillrWeather Spark Runner",
    libraryDependencies ++= app)
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7")
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .settings(dependencyDotFile := file("dependencies.dot"))
  .settings(
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather Spark uber jar",
    packageDescription := "KillrWeather Spark uber jar",
    assemblyJarName in assembly := "killrweather-spark.jar",
    mainClass in assembly := Some("com.lightbend.killrweather.app.KillrWeather"),
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH(assembly.value, "/var/www/html/")
    )
  )
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)

lazy val appLocalRunner = (project in file("./killrweather-app-local"))
  .settings(
    libraryDependencies ++= spark.map(_.copy(configurations = Option("compile")))
  )
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .dependsOn(killrWeatherApp)


lazy val loader = (project in file("./killrweather-loader"))
  .settings(defaultSettings:_*)
  .settings(
    buildInfoPackage := "build",
    packageName := "loader",
    mainClass in Compile := Some("com.lightbend.killrweather.loader.kafka.KafkaDataIngester"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather loaders",
    packageDescription := "KillrWeather loaders",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    ),
    libraryDependencies ++= loaders
  )
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val modelserver = (project in file("./killrweather-modelserver"))
  .settings(defaultSettings:_*)
  .settings(
    buildInfoPackage := "build",
    packageName := "modelserver",
    mainClass in Compile := Some("com.lightbend.killrweather.daily.server.modelserver.AkkaModelServer"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather HTTP client",
    packageDescription := "KillrWeather HTTP client",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    ),
    libraryDependencies ++= model
  )
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)

lazy val modelListener = (project in file("./killrweather-modellistener"))
  .settings(defaultSettings:_*)
  .settings(
    buildInfoPackage := "build",
    packageName := "modellistener",
    mainClass in Compile := Some("com.lightbend.killrweather.client.http.RestAPIs"),
    maintainer := "Boris Lublinsky <boris.lublinsky@lightbend.com",
    packageSummary := "KillrWeather HTTP client",
    packageDescription := "KillrWeather HTTP client",
    deployResourceConfigFiles ++= Seq("deploy.conf"),
    deployArtifacts ++= Seq(
      ArtifactSSH((packageZipTarball in Universal).value, "/var/www/html/")
    ),
    libraryDependencies ++= clientHTTP
  )
  .dependsOn(killrWeatherCore, protobufs)
  .enablePlugins(DeploySSH)
  .enablePlugins(JavaAppPackaging)


lazy val killrweathereventstore = (project in file("."))
  .aggregate(killrWeatherCore, killrWeatherApp,loader, modelListener, modelserver, protobufs)

