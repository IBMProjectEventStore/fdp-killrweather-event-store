# KillrWeather Event Store

KillrWeather is a reference application that is adapted from [Datastax's original KillrWeather application](https://github.com/killrweather/killrweather)). It shows how to easily leverage and integrate the following services for fast, streaming computations. This application focuses on the use case of [time series data](https://github.com/killrweather/killrweather/wiki/4.-Time-Series-Data-Model).

* [DB2 Event Store](https://www.ibm.com/us-en/marketplace/db2-event-store)
* [IBM Data Science Experience local](https://datascience.ibm.com/local)
* [Apache Spark](http://spark.apache.org)
* [Apache Kafka](http://kafka.apache.org),
* [Akka](https://akka.io/)
* [Grafana](https://grafana.com/)

This application can also be viewed as a prototypical IoT (or sensors) data collection application, which stores data in the form of a time series.

> **Disclaimer:** This sample application is provided as-is, without warranty. It is intended to illustrate techniques for implementing various scenarios using Fast Data Platform, but it has not gone through a robust validation process, nor does it use all the techniques commonly employed for highly-resilient, production applications. Please use it with appropriate caution.

## Sample Use Case

_I need fast access to real time data to analyze it, execute machine learning and leverage these models for predictive analytics._

## Reference Implementation

Overall architecture of the implementation looks as follows

![](diagrams/KillrWeatherES.png)

There are several modules in this application:

* [KillrWeather App](https://github.com/lightbend/fdp-killrweather-event-store/tree/develop/killrweather-app/src/main) is based on Spark Streaming and is responsible for basic processing of incoming data and storing it to the IBM DB2 Event Store
* [Data Loader](https://github.com/lightbend/fdp-killrweather-event-store/tree/develop/killrweather-loader/src/main) is a data loader (sensor simulator) for killrweather application, based on Akka Stream.
* [Model Listener](https://github.com/lightbend/fdp-killrweather-event-store/tree/develop/killrweather-modellistener/src/main) is a model updates HTTP listener based on Akka Streams, responsible for accepting model updates from IBM DSX.
* [Model Server](https://github.com/lightbend/fdp-killrweather-event-store/tree/develop/killrweather-modelserver/src/main) is an implementation of model serving, based on Akka Streams, responsible for weather prediction based on the model, generated leveraging IBM DSX.

## Prerequisites

Two Fast Data Platform services are required: Kafka and HDFS. (While Spark is also used, the DC/OS Spark _service_ is not required.) Follow the instructions in the Fast Data Platform documentation for installing the platform and these two components.

## Configuring application

The applications are configured using [typesafe config](https://github.com/lightbend/config).
Configuration file are located at the resources directory of every module. Examples of these config files are provided.

## Building and configuring applications

Applications are built using SBT and leverages [SBT Docker plugin](https://github.com/marcuslonnberg/sbt-docker).
It supports several commands:

* `sbt docker` builds a docker image locally
* `sbt dockerPush` pushes an image to the dockerHub
* `sbt dockerBuildAndPush` builds image and pushes it to the dockerHub

SBT is building the following Docker images:

* `lightbend/fdp-killrweather-event-store-model-listener`
* `lightbend/fdp-killrweather-event-store-loader`
* `lightbend/fdp-killrweather-event-store-app`
* `lightbend/fdp-killrweather-event-store-model-server`

## Deploying The applications to FDP

The following templates for deploying application to DC/OS are provided:

* KillrWeather App: `killrweather-app/src/main/resources/killrweatherAppDocker.json.template`
* Data Loader: `killrweather-loader/src/main/resources/killrweatherloaderDocker.json.template`
* Model Listener: `killrweather-modellistener/src/main/resources/killrweatheModelListenerDocker.json.template`
* Model Server: `killrweather-modelserver/src/main/resources/killrweatheModelServerDocker.json.template`

Run the following script to generate the JSON files from the templates, using the appropriate value for `VERSION`, e.g., `1.3.0`:

```bash
./process-templates.sh VERSION
```

Now you can deploy these applications to Fast Data Platform as follows, starting with the loader:

```bash
dcos marathon pod add killrweather-loader/src/main/resources/killrweatherloaderDocker.json
dcos marathon app add killrweather-modelserver/src/main/resources/killrweatheModelServerDocker.json
dcos marathon app add killrweather-modellistener/src/main/resources/killrweatheModelListenerDocker.json
dcos marathon app add killrweather-app/src/main/resources/killrweatherAppDocker.json
```

## See What's Going On

Use the EventStore UI to see the data ingestion in progress.

## Monitoring and Viewing Results

Monitoring is done using EventStore and Grafana.

## Running locally

In order to run locally, it is necessary first to install Eventstore and Grafana
1. To install EventStore follow instructions [here](https://github.com/IBMProjectEventStore/db2eventstore-enablement/tree/master/ReactiveSummit2018#installing-ibm-db2-event-store)
2. To install and configure Grafana follow instructions [here](https://github.com/IBMProjectEventStore/db2eventstore-enablement/tree/master/ReactiveSummit2018#grafana-integration)

Once this is done, the easiest way to run things is through IntelliJ.
Alternatively, if you want to use straight SBT, use something like command below

````
sbt '; project appLocalRunner; eval System.setProperty("config.resource", "localWithCluster.conf") ; runMain com.lightbend.killrweather.app.KillrWeatherEventStore'
````

This is the pattern to run all of the applications