package com.airwallex.codechallenge.input

import com.airwallex.codechallenge.AlertProcessor
import com.airwallex.codechallenge.alerters.TrendingAlerter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class TrendingAlerterTests {

    @Test
    fun `test simple fixed trend results in no alert`() {
        val start = Instant.parse("2000-01-01T00:00:00.000Z")
        val minimumTrendPeriod = 5L
        val throttlePeriod = 6

        val totalPeriods = 100

        val rates = InputBuilder(totalPeriods).buildLinearTrend(0, start)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(TrendingAlerter(minimumTrendPeriod, throttlePeriod))
        ) { alerts.add(it) }

        assertThat(alerts).isEmpty()
    }

    @Test
    fun `test simple rising linear trend results in one alert`() {
        val start = Instant.parse("2000-01-01T00:00:00.000Z")
        val minimumTrendPeriod = 5L
        val throttlePeriod = 6

        // as this is the trend between points we need one more than the minimum
        val totalPeriods = minimumTrendPeriod + 1

        val rates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(1, start)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(TrendingAlerter(minimumTrendPeriod, throttlePeriod))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(1)

        val alert = alerts.first()
        assertThat(alert.alert).isEqualTo("rising")
        assertThat(alert.timestamp).isEqualTo(start.plusSeconds(minimumTrendPeriod + 1))
        assertThat(alert.seconds).isEqualTo(minimumTrendPeriod)
    }

    @Test
    fun `test long rising linear trend results in one alert due to throttle`() {
        val start = Instant.parse("2000-01-01T00:00:00.000Z")
        val minimumTrendPeriod = 5L
        val throttlePeriod = 50
        val totalPeriods = 50L

        val rates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(1, start)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(TrendingAlerter(minimumTrendPeriod, throttlePeriod))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(1)

        val alert = alerts.first()
        assertThat(alert.alert).isEqualTo("rising")
        assertThat(alert.timestamp).isEqualTo(start.plusSeconds(minimumTrendPeriod + 1))
        assertThat(alert.seconds).isEqualTo(minimumTrendPeriod)
    }

    @Test
    fun `test long rising linear trend results in multiple alerts`() {
        val start = Instant.parse("2000-01-01T00:00:00.000Z")
        val minimumTrendPeriod = 5L
        val throttlePeriod = 10
        val totalPeriods = 50L

        val rates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(1, start)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(TrendingAlerter(minimumTrendPeriod, throttlePeriod))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(5)

        alerts.forEachIndexed { index, alert ->

            assertThat(alert.alert).isEqualTo("rising")

            // first alert is unthrottled so should occur as soon as the minimum trend is reached
            if (index == 0) {
                assertThat(alert.seconds).isEqualTo(minimumTrendPeriod)
                assertThat(alert.timestamp).isEqualTo(start.plusSeconds(minimumTrendPeriod + 1))
            } else {
                assertThat(alert.seconds).isEqualTo(minimumTrendPeriod + (index * throttlePeriod))
                assertThat(alert.timestamp).isEqualTo(start.plusSeconds(minimumTrendPeriod + 1 + (index.toLong() * throttlePeriod)))
            }
        }
    }

    @Test
    fun `test multiple currency pairs produce multiple alerts`() {
        val start = Instant.parse("2000-01-01T00:00:00.000Z")
        val minimumTrendPeriod = 5L
        val throttlePeriod = 6

        // as this is the trend between points we need one more than the minimum
        val totalPeriods = minimumTrendPeriod + 1

        val cnyAudRates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(1, start, 0.0, "CNYAUD")

        val audNzdRates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(-1, start, 0.0, "AUDNZD")

        val rates = mutableListOf<CurrencyConversionRate>()
        rates.addAll(cnyAudRates)
        rates.addAll(audNzdRates)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(TrendingAlerter(minimumTrendPeriod, throttlePeriod))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(2)

        val first = alerts.first()
        assertThat(first.alert).isEqualTo("rising")
        assertThat(first.currencyPair).isEqualTo("CNYAUD")
        assertThat(first.timestamp).isEqualTo(start.plusSeconds(minimumTrendPeriod + 1))
        assertThat(first.seconds).isEqualTo(minimumTrendPeriod)

        val second = alerts[1]
        assertThat(second.alert).isEqualTo("falling")
        assertThat(second.currencyPair).isEqualTo("AUDNZD")
        assertThat(second.timestamp).isEqualTo(start.plusSeconds(minimumTrendPeriod + 1))
        assertThat(second.seconds).isEqualTo(minimumTrendPeriod)
    }
}
