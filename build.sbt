import Dependencies._

scalaVersion in ThisBuild := "2.11.12"
scalacOptions in ThisBuild ++= Seq("-target:jvm-1.8")

autoScalaLibrary := false
libraryDependencies += "org.scala-lang" % "scala-library" % scalaVersion.value % "provided"

version in ThisBuild := "1.2.0"
organization in ThisBuild := "lightbend"
//val K8S_OR_DCOS = ""//"K8S"


// settings for a native-packager based docker scala project based on sbt-docker plugin
def sbtdockerScalaAppBase(id: String)(base: String = id) = Project(id, base = file(base))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    dockerfile in docker := {
      val appDir = stage.value
      val targetDir = s"/$base"

      new Dockerfile {
        from("openjdk:8u151-jre")
        entryPoint(s"$targetDir/bin/${executableScriptName.value}")
        copy(appDir, targetDir)
      }
    },

    // Set name for the image
    imageNames in docker := Seq(
      ImageName(namespace = Some(organization.value),
        repository = name.value.toLowerCase,
        tag = Some(version.value))
    ),

    buildOptions in docker := BuildOptions(cache = false)
  )

def sbtdockerSparkAppBase(id: String)(base: String = id) = Project(id, base = file(base))
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)
  .settings(
    dockerfile in docker := {
      val artifact: File = assembly.value
      val artifactTargetPath = s"/opt/spark/jars/${artifact.name}"
/*
      K8S_OR_DCOS match {
        case "K8S" =>
          new Dockerfile {
            from ("gcr.io/ynli-k8s/spark:v2.3.0")           // K8
            add(artifact, artifactTargetPath)
            runRaw("mkdir -p /etc/hadoop/conf")
            runRaw("export HADOOP_CONF_DIR=/etc/hadoop/conf")
          }

        case _ => */
          new Dockerfile {
            from ("mesosphere/spark:2.3.0-2.2.1-2-hadoop-2.6")    // DC/OS
            add(artifact, artifactTargetPath)
            runRaw("mkdir -p /etc/hadoop/conf")
            runRaw("export HADOOP_CONF_DIR=/etc/hadoop/conf")
          }
//      }
    },

    // Set name for the image
    imageNames in docker := Seq(
      ImageName(namespace = Some(organization.value),
        repository = /*if (K8S_OR_DCOS =="K8S") s"${name.value.toLowerCase}-k8s" else */ name.value.toLowerCase,
        tag = Some(version.value))
    ),

    buildOptions in docker := BuildOptions(cache = false)
  )

lazy val protobufs = (project in file("./protobufs"))
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ))

// Supporting project - used as an internal library
lazy val killrWeatherCore = (project in file("./killrweather-core"))
  .settings(libraryDependencies ++= core)


// Spark streaming project
lazy val killrWeatherApp = sbtdockerSparkAppBase("killrWeatherAppES")("./killrweather-app")
  .settings(libraryDependencies ++= app)
  .settings (mainClass in Compile := Some("com.lightbend.killrweather.app.KillrWeatherEventStore"))
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .settings(
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
      case PathList("META-INF", xs @ _*) => MergeStrategy.last
      case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    }
  )
  .dependsOn(killrWeatherCore, protobufs)

// Supporting projects to enable running spark projects locally
lazy val appLocalRunner = (project in file("./killrweather-app-local"))
  .settings(
      libraryDependencies ++= spark.map(_.withConfigurations(configurations = Option("compile")))
  )
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.6.7",
    dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.6.7")
  .dependsOn(killrWeatherApp)

// Loader - loading weather data to Kafka - pure scala
lazy val loader = sbtdockerScalaAppBase("loader")("./killrweather-loader")
  .settings(
    mainClass in Compile := Some("com.lightbend.killrweather.loader.kafka.KafkaDataIngester"),
    libraryDependencies ++= loaders,
    bashScriptExtraDefines += """addJava "-Dconfig.resource=cluster.conf""""
  )
  .dependsOn(killrWeatherCore, protobufs)

// Model Server - Real time model scoring - pure scala
lazy val modelserver = sbtdockerScalaAppBase("modelserverES")("./killrweather-modelserver")
  .settings(
    mainClass in Compile := Some("com.lightbend.killrweather.daily.server.modelserver.AkkaModelServer"),
    libraryDependencies ++= model ++ spark.map(_.withConfigurations(configurations = Option("compile"))),
    bashScriptExtraDefines += """addJava "-Dconfig.resource=cluster.conf""""
  )
  .settings(dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-core"  % "2.9.1",
            dependencyOverrides += "com.fasterxml.jackson.core" % "jackson-databind" % "2.9.1")

  .dependsOn(killrWeatherCore, protobufs)

// Model Listener - Listen to model updates and write them to Kafka - pure scala
lazy val modelListener = sbtdockerScalaAppBase("modelListenerES")("./killrweather-modellistener")
  .settings(
    mainClass in Compile := Some("com.lightbend.killrweather.modellistener.TemperaturePredictionModel"),
    libraryDependencies ++= clientHTTP,
    bashScriptExtraDefines += """addJava "-Dconfig.resource=cluster.conf""""
  )
  .dependsOn(killrWeatherCore, protobufs)


lazy val killrweathereventstore = (project in file("."))
  .aggregate(killrWeatherCore, killrWeatherApp,loader, modelListener, modelserver, protobufs)

