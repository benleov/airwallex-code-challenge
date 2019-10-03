package com.airwallex.codechallenge.alerters

import com.airwallex.codechallenge.input.Alert
import com.airwallex.codechallenge.input.CurrencyConversionRate
import kotlin.math.absoluteValue

/**
 * Required periods is moving average periods size plus one for the tested period. I.e to test a moving average of 5
 * specify 6.
 */
class MovingAverageAlerter(
    requiredPeriods: Int,
    private val percentageAlertThreshold: Double
) : Alerter(requiredPeriods) {

    /**
     * Produces an alert when the spot rate for a currency pair changes by more than 10% from the 5 minute average
     * for the specified currency pair.
     */
    override fun hasAlert(currencyPair: String, rates: List<CurrencyConversionRate>): Alert? {

        // calculate moving average over defined period, excluding the latest period
        val average = rates
            .subList(0, rates.size - 1)
            .map { it.rate }
            .reduce { acc, next -> acc + next }
            .toDouble() / (requiredPeriods - 1)

        val latestRate = rates.last()

        // check difference between moving average and latest period
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