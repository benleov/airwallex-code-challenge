package com.airwallex.codechallenge.alerters

import com.airwallex.codechallenge.input.Alert
import com.airwallex.codechallenge.input.CurrencyConversionRate
import java.time.Duration
import java.time.Instant


class TrendingAlerter(
    private val minimumTrendLength: Long = 900,
    private val throttlePeriodSeconds: Int = 60
) : Alerter(1) {

    private var currentTrend: Trend? = null
    private var lastConversionRate: CurrencyConversionRate? = null
    private var lastAlertTime: Instant? = null

    data class Trend(val trendingUp: Boolean, val start: Instant)

    /** Produces an alert when the spot rate has been rising/falling for minimumTrendLength seconds.
     *
     * This alert is throttled to only output once per throttlePeriodSeconds and reports the length of time
     * of the rise/fall in seconds.
     */
    override fun hasAlert(currencyPair: String, rates: List<CurrencyConversionRate>): Alert? {

        if (rates.size != 1) {
            throw RuntimeException("Trending alerter expects only one rate")
        }

        val currentRate = rates.first()
        val currentTime = currentRate.timestamp

        if (lastConversionRate == null) {
            lastConversionRate = currentRate
        } else if (currentRate.rate != lastConversionRate!!.rate) {

            val trendingUp = currentRate.rate > lastConversionRate!!.rate

            if (currentTrend == null) {
                currentTrend = Trend(trendingUp, lastConversionRate!!.timestamp)
            } else if (trendingUp xor currentTrend!!.trendingUp) {
                // trend changed direction
                currentTrend = Trend(trendingUp, lastConversionRate!!.timestamp)
            }

            // determine if alert should be produced; is the trend long enough and has it been throttled
            val trendLength = Duration.between(currentTrend!!.start, currentRate.timestamp).seconds
            lastConversionRate = currentRate

            if (trendLength >= minimumTrendLength) {
                // only once per minute
                if (lastAlertTime == null || Duration.between(
                        lastAlertTime,
                        currentTime
                    ).seconds >= throttlePeriodSeconds
                ) {
                    lastAlertTime = currentTime
                    return Alert(
                        currentTime,
                        currencyPair,
                        if (currentTrend!!.trendingUp) "rising" else "falling",
                        trendLength
                    )
                }
            }
        } else {
            // pair is identical to last period
            lastConversionRate = currentRate
        }
        return null
    }

    override fun clone(): Alerter {
        return TrendingAlerter(minimumTrendLength, throttlePeriodSeconds)
    }
}
