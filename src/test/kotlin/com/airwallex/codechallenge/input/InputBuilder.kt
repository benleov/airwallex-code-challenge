package com.airwallex.codechallenge.input

import java.time.Instant
import java.time.Instant.now

class InputBuilder(
    private val periods: Int = 50
) {
    /**
     * Build a list of conversion rates for a single pair that increases linearly.
     */
    fun buildLinearTrend(
        increment: Int = 1,
        startTime: Instant = now(),
        startRate: Double = 0.0,
        currencyPair: String = "AUDNZD"
    ): List<CurrencyConversionRate> {

        var rate = startRate
        return (0 until periods).map {
            CurrencyConversionRate(
                timestamp = startTime.plusSeconds(it.toLong() + 1),
                currencyPair = currencyPair,
                rate = rate + (increment * it)
            )
        }
    }

    /**
     * Build a list of conversion rates for a single pair that increases linearly with a spike increase every
     * interval (basically a slope with steps).
     */
    fun buildLinearWithPercentageSpikeEveryInterval(
        interval: Int,
        start: Instant = now(),
        percentageSpike: Double,
        linearIncrement: Double
    ): List<CurrencyConversionRate> {

        val currencyPair = "AUDNZ"
        var rate = linearIncrement

        return (1 until periods + 1).map { index ->

            // every interval increase by the linear increment as well as a percentage of the total
            rate += if (index % interval == 0) linearIncrement + ((percentageSpike / 100) * rate) else linearIncrement

            CurrencyConversionRate(
                timestamp = start.plusSeconds(index.toLong()),
                currencyPair = currencyPair,
                rate = rate
            )
        }
    }

    /**
     * Flat rate with periodic spikes.
     */
    fun buildFixedRateWithFixedSpikeEveryInterval(
        interval: Int,
        start: Instant = now(),
        fixedSpike: Double,
        fixedRate: Double,
        currencyPair: String = "AUDNZD"
    ): List<CurrencyConversionRate> {

        return (1 until periods + 1).map { index ->

            val rate = if (index % interval == 0) fixedSpike else fixedRate

            CurrencyConversionRate(
                timestamp = start.plusSeconds(index.toLong()),
                currencyPair = currencyPair,
                rate = rate
            )
        }
    }

}