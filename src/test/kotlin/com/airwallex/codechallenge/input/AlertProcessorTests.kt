package com.airwallex.codechallenge.input

import com.airwallex.codechallenge.AlertProcessor
import com.airwallex.codechallenge.alerters.MovingAverageAlerter
import com.airwallex.codechallenge.alerters.TrendingAlerter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class AlertProcessorTests {

    @Test
    fun `test multiple currencies and multiple processors`() {
        val startTime = Instant.parse("2000-01-01T00:00:00.000Z")

        // moving average
        val requiredPeriods = 10 // moving average of 9
        val alertThreshold = 70.0

        // trending
        val minimumTrendPeriod = 5L
        val throttlePeriod = 10

        // rates
        val totalPeriods = 20L
        val linearIncrement = 1.0

        val spikeInterval = 10   // spike interval
        val percentageSpike = 10.0

        val audNzdRates = InputBuilder((totalPeriods).toInt())
            .buildLinearWithPercentageSpikeEveryInterval(spikeInterval, startTime, percentageSpike, linearIncrement, "AUDNZD")
        val cnyaudRates = InputBuilder((totalPeriods).toInt())
            .buildLinearWithPercentageSpikeEveryInterval(spikeInterval, startTime, percentageSpike, linearIncrement, "CNYAUD")

        val rates = mutableListOf<CurrencyConversionRate>().also {
            it.addAll(audNzdRates)
            it.addAll(cnyaudRates)
        }

        rates.forEach { println(it) }

        println()

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(
                MovingAverageAlerter(requiredPeriods, alertThreshold),
                TrendingAlerter(minimumTrendPeriod, throttlePeriod)
            )
        ) { alerts.add(it) }

        // there should be 4 trending alerts; one at 5 seconds for each currency (initial non throttled)
        // and one at 15 seconds

        // there should be

        alerts.forEach { println(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(4)

        val alert = alerts.first()
        assertThat(alert.alert).isEqualTo("spotChange")
        assertThat(alerts.map { alert }.toSet()).size().isEqualTo(1)
    }
}