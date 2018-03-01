package com.lightbend.killrweather.app.eventstore

import com.ibm.event.oltp.{EventContext, InsertResult}
import com.lightbend.killrweather.EventStore.EventStoreSupport
import org.apache.spark.sql.Row

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object EventStoreSink {

  def apply(eventStoreConfiguration: Option[String]): EventStoreSink = {
    val f = () => {
      val ctx = EventStoreSupport.createContext(eventStoreConfiguration)
      EventStoreSupport.ensureTables(ctx)
      sys.addShutdownHook {
        EventContext.cleanUp()
      }
      ctx
    }
    new EventStoreSink(f)
  }
}

class EventStoreSink(createContext: () => EventContext) extends Serializable {

  import com.lightbend.killrweather.settings.WeatherSettings._

  lazy val ctx = createContext()

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
    val dataSeq = data.toIndexedSeq
    if(dataSeq.size > 0) {
      try {
        val table = ctx.getTable(tableName)
        val future: Future[InsertResult] = ctx.batchInsertAsync(table, dataSeq)
        val result: InsertResult = Await.result(future, Duration.Inf)
        if (result.failed) {
          println(s"batch insert incomplete: $result")
        }
      }catch {
        case t : Throwable => printf(s"Error writing to eventStore $t")
      }
    }
  }
}