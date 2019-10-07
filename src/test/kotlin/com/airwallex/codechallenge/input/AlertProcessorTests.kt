package com.airwallex.codechallenge.input

import com.airwallex.codechallenge.AlertProcessor
import com.airwallex.codechallenge.alerters.MovingAverageAlerter
import com.airwallex.codechallenge.alerters.TrendingAlerter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class AlertProcessorTests {

   @Test
   fun `test multiple common processors and multiple currencies`() {
     val startTime = Instant.parse("2000-01-01T00:00:00.000Z")

     // moving average
     val movingAverageLength = 5
     val alertThreshold = 50.0

     // rates
     val totalPeriods = 20L
     val percentageIncrement = 0.01

     val spikeInterval = 10
     val percentageSpike = 70.0

     val audNzdRates = InputBuilder((totalPeriods).toInt())
       .buildPercentageIncreaseWithPercentageSpikeEveryInterval(
         spikeInterval, startTime, percentageSpike, percentageIncrement, "AUDNZD"
       )
     val cnyaudRates = InputBuilder((totalPeriods).toInt())
       .buildPercentageIncreaseWithPercentageSpikeEveryInterval(
         spikeInterval, startTime, percentageSpike, percentageIncrement, "CNYAUD"
       )

     val rates = mutableListOf<CurrencyConversionRate>().also {
       it.addAll(audNzdRates)
       it.addAll(cnyaudRates)
     }

     val alerts = mutableListOf<Alert>()

     AlertProcessor().process(
       rates,
       listOf(
         MovingAverageAlerter(movingAverageLength, alertThreshold),
         MovingAverageAlerter(movingAverageLength, alertThreshold),
         MovingAverageAlerter(movingAverageLength, alertThreshold)
       )
     ) { alerts.add(it) }
   }

    @Test
    fun `test multiple currencies and multiple processors`() {
        val startTime = Instant.parse("2000-01-01T00:00:00.000Z")

        // moving average
        val movingAverageLength = 5
        val alertThreshold = 50.0

        // trending (rising/falling)
        val minimumTrendPeriod = 5L
        val throttlePeriod = 10

        // rates
        val totalPeriods = 20L
        val percentageIncrement = 0.01

        val spikeInterval = 10
        val percentageSpike = 70.0

        val audNzdRates = InputBuilder((totalPeriods).toInt())
            .buildPercentageIncreaseWithPercentageSpikeEveryInterval(
                spikeInterval, startTime, percentageSpike, percentageIncrement, "AUDNZD"
            )
        val cnyaudRates = InputBuilder((totalPeriods).toInt())
            .buildPercentageIncreaseWithPercentageSpikeEveryInterval(
                spikeInterval, startTime, percentageSpike, percentageIncrement, "CNYAUD"
            )

        val rates = mutableListOf<CurrencyConversionRate>().also {
            it.addAll(audNzdRates)
            it.addAll(cnyaudRates)
        }

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(
                MovingAverageAlerter(movingAverageLength, alertThreshold),
                TrendingAlerter(minimumTrendPeriod, throttlePeriod)
            )
        ) { alerts.add(it) }

        // there should be 4 trending alerts; one at 5 seconds for each currency (initial non throttled)
        // and one at 15 seconds (throttle is 10 seconds), and two spot changes, one for each spike

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(8)

        for (currencyPair in listOf("AUDNZD", "CNYAUD")) {
            val currencyAlerts = alerts.filter { it.currencyPair == currencyPair }

            val spotChanges = currencyAlerts.filter { it.alert == "spotChange" }
            assertThat(spotChanges.size).isEqualTo(2)
            assertThat(spotChanges.first().timestamp).isEqualTo(Instant.parse("2000-01-01T00:00:10Z"))
            assertThat(spotChanges.last().timestamp).isEqualTo(Instant.parse("2000-01-01T00:00:20Z"))

            val trendingAlerts = currencyAlerts.filter { it.alert == "rising" }
            assertThat(trendingAlerts.size).isEqualTo(2)
            assertThat(trendingAlerts.first().timestamp).isEqualTo(Instant.parse("2000-01-01T00:00:06Z"))
            assertThat(trendingAlerts.first().seconds).isEqualTo(5)

            assertThat(trendingAlerts.last().timestamp).isEqualTo(Instant.parse("2000-01-01T00:00:16Z"))
            assertThat(trendingAlerts.last().seconds).isEqualTo(15)
        }
    }
}
