package com.airwallex.codechallenge.alerters

import com.airwallex.codechallenge.input.Alert
import com.airwallex.codechallenge.input.CurrencyConversionRate
import kotlin.math.absoluteValue

/**
 *
 */
class MovingAverageAlerter(
    movingAverageLength: Int,
    private val percentageAlertThreshold: Double
) : Alerter(movingAverageLength + 1) { // required periods is moving average periods size plus one for the tested period

    var sum: Double? = null
    var lastValue: Double? = null

    /**
     * Produces an alert when the spot rate for a currency pair changes by more than 10% from the 5 minute average
     * for the specified currency pair.
     *
     * Note: it is expected that this is called with a moving window of rates.
     */
    override fun hasAlert(currencyPair: String, rates: List<CurrencyConversionRate>): Alert? {

        // calculate moving average over defined period, excluding the latest period
        val averageValues = rates.subList(0, rates.size - 1)

        if(sum == null) {
          // first time through, calculate the sum
          sum = averageValues
            .map { it.rate }
            .reduce { acc, next -> acc + next }
        } else {
          // next time through, don't need to add all the values together, just subtract the last value (which has fallen off the end
          // of the window), and add the newest one
          sum = sum!!.minus(lastValue!!)
          sum = sum!!.plus(averageValues[averageValues.size - 1].rate)
        }

        lastValue = averageValues.first().rate

        val average = sum!! / (rates.size - 1).toDouble()

        val latestRate = rates.last()

        // check percentage difference between moving average and latest period
        val difference = (((average - latestRate.rate) / ((average + latestRate.rate) / 2)).absoluteValue)

        if (difference >= (percentageAlertThreshold / 100)) {
            return Alert(latestRate.timestamp, currencyPair, "spotChange")
        }

        return null
    }

    override fun clone(): Alerter {
        return MovingAverageAlerter(requiredPeriods, percentageAlertThreshold)
    }
}