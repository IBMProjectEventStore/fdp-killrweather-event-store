package com.lightbend.killrweather.app.eventstore

import com.ibm.event.oltp.{EventContext, InsertResult}
import com.lightbend.killrweather.EventStore.EventStoreSupport
import com.lightbend.killrweather.settings.WeatherSettings._
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

  var ctx = createContext()

  def writeRaw(raw: Iterator[Row]): Unit = {
    writeBatch(RAWWEATHER, raw)
  }

  def writeDailyTemperature(raw: Iterator[Row]): Unit = {
    writeBatch(DAYLYTEMP, raw)
  }

  def writeDailyWind(raw: Iterator[Row]): Unit = {
    writeBatch(DAYLYWIND, raw)
  }

  def writeDailyPressure(raw: Iterator[Row]): Unit = {
    writeBatch(DAYLYPRESS, raw)
  }

  def writeDailyPresip(raw: Iterator[Row]): Unit = {
    writeBatch(DAYLYPRECIP, raw)
  }

  def writeMothlyTemperature(raw: Iterator[Row]): Unit = {
    writeBatch(MONTHLYTEMP, raw)
  }

  def writeMothlyWind(raw: Iterator[Row]): Unit = {
    writeBatch(MONTHLYWIND, raw)
  }

  def writeMothlyPressure(raw: Iterator[Row]): Unit = {
    writeBatch(MONTHLYPRESS, raw)
  }

  def writeMothlyPresip(raw: Iterator[Row]): Unit = {
    writeBatch(MONTHLYPRECIP, raw)
  }

  private def writeBatch(tableName : String, data: Iterator[Row]) : Unit = {
    // Ensure that  that we are connected
    if (!ctx.isDefined) {
      println(s"Recreating the Event Store Context!!!")
      ctx = EventStoreSupport.createContext(eventStore, user, password)
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