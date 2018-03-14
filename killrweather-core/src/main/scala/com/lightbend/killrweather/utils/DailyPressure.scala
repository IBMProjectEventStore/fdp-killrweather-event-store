package com.lightbend.killrweather.utils

/**
 * Created by boris on 7/19/17.
 */
case class DailyPressure(
  wsid: Long,
  year: Int,
  month: Int,
  day: Int,
  ts: Long,
  high: Double,
  low: Double,
  mean: Double,
  variance: Double,
  stdev: Double
) extends Serializable

object DailyPressure {
  def apply(daily: DailyWeatherData): DailyPressure =
    new DailyPressure(daily.wsid, daily.year, daily.month, daily.day, daily.ts, daily.highPressure, daily.lowPressure,
      daily.meanPressure, daily.variancePressure, daily.stdevPressure)
}

case class MonthlyPressure(
  wsid: Long,
  year: Int,
  month: Int,
  ts: Long,
  high: Double,
  low: Double,
  mean: Double,
  variance: Double,
  stdev: Double
) extends Serializable

object MonthlyPressure {
  def apply(monthly: MonthlyWeatherData): MonthlyPressure =
    new MonthlyPressure(monthly.wsid, monthly.year, monthly.month, monthly.ts, monthly.highPressure, monthly.lowPressure,
      monthly.meanPressure, monthly.variancePressure, monthly.stdevPressure)
}