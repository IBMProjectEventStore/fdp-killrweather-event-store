import sbt._
import Versions._

object Dependencies {

  implicit class Exclude(module: ModuleID) {
    def log4jExclude: ModuleID =
      module excludeAll(ExclusionRule("log4j"))

    def driverExclusions: ModuleID =
      module.log4jExclude.exclude("com.google.guava", "guava")
        .excludeAll(ExclusionRule("org.slf4j"))
  }

  val akkaStream        = "com.typesafe.akka"       % "akka-stream_2.11"                % AkkaStreams
  val akkaStreamKafka   = "com.typesafe.akka"       % "akka-stream-kafka_2.11"          % AkkaStreamsKafka
  val akkaHttpCore      = "com.typesafe.akka"       % "akka-http_2.11"                  % AkkaHTTP
  val akkaActor         = "com.typesafe.akka"       % "akka-actor_2.11"                 % Akka
  val akkaSlf4j         = "com.typesafe.akka"       % "akka-slf4j_2.11"                 % Akka
  val akkaHttpJsonJackson = "de.heikoseeberger"     %% "akka-http-jackson"              % akkaHttpJsonVersion

  val curator           = "org.apache.curator"      % "curator-test"                    % Curator                           // ApacheV2
  val jodaTime          = "joda-time"               % "joda-time"                       % JodaTime                          // ApacheV2
  val jodaConvert       = "org.joda"                % "joda-convert"                    % JodaConvert                       // ApacheV2
  val json4sCore        = "org.json4s"              % "json4s-core_2.11"                % Json4s                            // ApacheV2
  val json4sJackson     = "org.json4s"              % "json4s-jackson_2.11"             % Json4s                            // ApacheV2
  val json4sNative      = "org.json4s"              % "json4s-native_2.11"              % Json4s                            // ApacheV2

  val kafka             = "org.apache.kafka"        % "kafka_2.11"                      % Kafka                             // ApacheV2

  val sparkCore         = "org.apache.spark"        % "spark-core_2.11"                 % Spark          % "provided"                   // ApacheV2
  val sparkCatalyst     = "org.apache.spark"        % "spark-catalyst_2.11"             % Spark          % "provided"                   // ApacheV2
  val sparkKafkaStreaming = "org.apache.spark"      % "spark-streaming-kafka-0-10_2.11" % Spark                             // ApacheV2
  val sparkStreaming    = "org.apache.spark"        % "spark-streaming_2.11"            % Spark          % "provided"                   // ApacheV2
  val sparkSQL          = "org.apache.spark"        % "spark-sql_2.11"                  % Spark          % "provided"                   // ApacheV2
  val sparkML           = "org.apache.spark"        % "spark-mllib_2.11"                % Spark          % "provided"

  val sparkKafkaSQL     = "org.apache.spark"        % "spark-sql-kafka-0-10_2.11"       % Spark

  val sparkJPPML        = "org.jpmml"               % "jpmml-sparkml"                   % SparkJPPML

  val logback           = "ch.qos.logback"          % "logback-classic"                 % Logback                           // LGPL
  val slf4jApi          = "org.slf4j"               % "slf4j-api"                       % Slf4j                             // MIT
  val slf4jLog          = "org.slf4j"               % "slf4j-log4j12"                   % Slf4j                             // MIT

  val scalaPBRuntime    = "com.trueaccord.scalapb"  %% "scalapb-runtime"                % com.trueaccord.scalapb.compiler.Version.scalapbVersion % "protobuf"
  val scalaPBJSON       = "com.trueaccord.scalapb"  %% "scalapb-json4s"                 % ScalaPBJSONVersion

  val jpmml         = "org.jpmml"                    % "pmml-evaluator"                % PMMLVersion
  val jpmmlextras   = "org.jpmml"                    % "pmml-evaluator-extension"      % PMMLVersion

  val typesafeConfig    = "com.typesafe"            %  "config"                         % TypesafeConfigVersion
  val ficus             = "com.iheart"              %% "ficus"                          % FicusVersion

  val logging = Seq(logback.exclude("org.slf4j", "slf4j-api"), slf4jApi)

  val akka = Seq(
    akkaActor,
    akkaHttpJsonJackson,
    akkaSlf4j.
      exclude("org.slf4j", "slf4j-api").
      exclude("org.slf4j", "slf4j-log4j12"),
    akkaHttpCore, akkaStream, akkaStreamKafka)

  val time = Seq(jodaConvert, jodaTime)
  val json = Seq(json4sCore, json4sJackson, json4sNative)
  val spark = Seq(sparkCore, sparkStreaming,
    sparkKafkaStreaming
      .exclude("org.apache.spark", "spark-tags_2.11")
      .exclude("org.apache.spark", "spark-streaming_2.11")
      .exclude("org.apache.kafka", "kafka_2.11")
      .exclude("org.spark-project.spark", "unused")
      .exclude("org.apache.spark", "spark-core_2.11"),
    sparkCatalyst, sparkSQL, sparkML, sparkJPPML)

  /** Module deps */
  val loaders = json
  val core = logging ++ time ++ spark ++ Seq(
    curator.
      exclude("io.netty", "netty"),
    kafka.
      exclude("org.slf4j", "slf4j-log4j12").
      exclude("io.netty", "netty"),
    typesafeConfig, ficus
  )
  val app = spark
  val clientHTTP = logging ++ akka ++ json
  val model = akka ++ Seq(jpmml, jpmmlextras)
}