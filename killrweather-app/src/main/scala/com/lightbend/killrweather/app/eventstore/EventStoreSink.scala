package com.lightbend.killrweather.app.eventstore

import com.ibm.event.oltp.{EventContext, InsertResult}
import com.lightbend.killrweather.EventStore.EventStoreSupport
import com.lightbend.killrweather.settings.WeatherSettings
import org.apache.spark.sql.Row

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object EventStoreSink {

  def apply(eventStoreConfiguration: String, user: String, password: String): EventStoreSink = {
    val f = () => {
      val ctx = EventStoreSupport.createContext(eventStoreConfiguration, user, password)
      sys.addShutdownHook {
        EventContext.cleanUp()
      }
      ctx
    }
    new EventStoreSink(f)
  }
}

class EventStoreSink(createContext: () => Option[EventContext]) extends Serializable {

  // Create context
  val settings = WeatherSettings()
  import settings._

  var ctx = createContext()

  def writeRaw(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.rawWeather, raw)
  }

  def writeDailyTemperature(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.daylyTemperature, raw)
  }

  def writeDailyWind(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.daylyWind, raw)
  }

  def writeDailyPressure(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.dailyPressure, raw)
  }

  def writeDailyPresip(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.dailyPrecipitation, raw)
  }

  def writeMothlyTemperature(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.monthlyTemperature, raw)
  }

  def writeMothlyWind(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.monthlyWind, raw)
  }

  def writeMothlyPressure(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.monthlyPressure, raw)
  }

  def writeMothlyPresip(raw: Iterator[Row]): Unit = {
    writeBatch(eventStoreTables.monthlyPrecipitation, raw)
  }

  private def writeBatch(tableName : String, data: Iterator[Row]) : Unit = {
    // Ensure that  that we are connected
    if (!ctx.isDefined) {
      println(s"Recreating the Event Store Context!!!")
      ctx = EventStoreSupport.createContext(eventStoreConfig.endpoint, eventStoreConfig.user, eventStoreConfig.password)
      EventStoreSupport.ensureTables(ctx.get)
    }

    // write
    val dataSeq = data.toIndexedSeq
    val start = System.currentTimeMillis()
    if (dataSeq.size > 0) {
        println(s"Inserting a new batch ${dataSeq.size}, table $tableName context = ${ctx.getOrElse(null)}  !!!!!!!")
        try {
          //val table = ctx.get.getTable(tableName)
          val future: Future[InsertResult] = ctx.get.batchInsertAsync(tableName, dataSeq, true)
          val result: InsertResult = Await.result(future, Duration.apply(1, SECONDS))
          if (result.failed) {
            println(s"batch insert incomplete: $result")
          }
        } catch {
          case t: Throwable =>
            printf(s"Error writing to eventStore $t")
            ctx = None
        }
        println(s"Done inserting batch, table $tableName  in ${System.currentTimeMillis() - start} !!!!!!!")
    }
  }
}