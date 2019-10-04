package com.airwallex.codechallenge.input

import java.time.Instant
import java.time.Instant.now

/**
 * Constructs mock conversion rates.
 */
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

    /**
     *
     */
    fun buildPercentageIncreaseWithPercentageSpikeEveryInterval(
        spikeInterval: Int,
        start: Instant = now(),
        percentageSpike: Double,
        percentageIncrement: Double,
        currencyPair: String = "AUDNZD"
    ): List<CurrencyConversionRate> {

        var rate = 1.0

        return (1 until periods + 1).map { index ->

            // every interval increase by the linear increment as well a percentage spike of the rate as it was
            // previously
            rate += if (index % spikeInterval == 0) (percentageIncrement * rate) + ((percentageSpike / 100) * (rate)) else percentageIncrement * rate

            CurrencyConversionRate(
                timestamp = start.plusSeconds(index.toLong()),
                currencyPair = currencyPair,
                rate = rate
            )
        }
    }
}