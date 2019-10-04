package com.airwallex.codechallenge.input

import com.airwallex.codechallenge.AlertProcessor
import com.airwallex.codechallenge.alerters.MovingAverageAlerter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class MovingAverageAlerterTests {

    @Test
    fun `test simple fixed trend results in no alert`() {
        val start = Instant.parse("2000-01-01T00:00:00.000Z")

        val movingAveragePeriods = 5
        val alertThreshold = 0.1
        val totalPeriods = 100

        val rates = InputBuilder(totalPeriods).buildLinearTrend(0, start)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(MovingAverageAlerter(movingAveragePeriods, alertThreshold))
        ) { alerts.add(it) }

        assertThat(alerts).isEmpty()
    }

    @Test
    fun `test small linear increment produces no alarms`() {
        val startTime = Instant.parse("2000-01-01T00:00:00.000Z")
        val totalPeriods = 50L
        val movingAverageLength = 5
        val alertThreshold = 5.0

        // 1% with relatively decreasing increase
        val increment = 1
        val startRate = 100.0
        val rates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(increment, startTime, startRate)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(MovingAverageAlerter(movingAverageLength, alertThreshold))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(0)
    }

    @Test
    fun `test large linear increment produces one alarm`() {
        val startTime = Instant.parse("2000-01-01T00:00:00.000Z")

        val totalPeriods = 5L
        val movingAverageLength = 4
        val alertThreshold = 5.0
        val increment = 1
        val startRate = 1.0

        val rates = InputBuilder((totalPeriods).toInt())
            .buildLinearTrend(increment, startTime, startRate)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(MovingAverageAlerter(movingAverageLength, alertThreshold))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(1)

        // the last value will alert as average is 2.5 and the value is 5; a 66% increase which is above the threshold
        val alert = alerts.first()
        assertThat(alert.alert).isEqualTo("spotChange")
        assertThat(alert.timestamp).isEqualTo(startTime.plusSeconds(totalPeriods))
    }

    @Test
    fun `test fixed rate with fixed spikes produces alarm per spike`() {
        val startTime = Instant.parse("2000-01-01T00:00:00.000Z")

        val totalPeriods = 21L
        val movingAverageLength = 9
        val alertThreshold = 70.0
        val interval = 10
        val fixedRate = 1.0
        val fixedSpike = 10.0

        val rates = InputBuilder((totalPeriods).toInt())
            .buildFixedRateWithFixedSpikeEveryInterval(interval, startTime, fixedSpike, fixedRate)

        val alerts = mutableListOf<Alert>()

        AlertProcessor().process(
            rates,
            listOf(MovingAverageAlerter(movingAverageLength, alertThreshold))
        ) { alerts.add(it) }

        assertThat(alerts).isNotNull
        assertThat(alerts).size().isEqualTo(2)

        val alert = alerts.first()
        assertThat(alert.alert).isEqualTo("spotChange")
        assertThat(alerts.map { alert }.toSet()).size().isEqualTo(1)
    }
}
