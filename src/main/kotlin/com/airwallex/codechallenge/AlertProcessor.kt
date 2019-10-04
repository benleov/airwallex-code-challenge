package com.airwallex.codechallenge

import com.airwallex.codechallenge.alerters.Alerter
import com.airwallex.codechallenge.alerters.MovingAverageAlerter
import com.airwallex.codechallenge.alerters.TrendingAlerter
import com.airwallex.codechallenge.input.Alert
import com.airwallex.codechallenge.input.CurrencyConversionRate
import com.airwallex.codechallenge.input.Mapper
import java.util.*

class AlertProcessor {

    fun process(rates: List<CurrencyConversionRate>, alerters: List<Alerter>, callback: (alert: Alert) -> Unit) {

        if (alerters.isEmpty()) {
            throw RuntimeException("No alerters have been specified.")
        }

        // this could be a bound/limited queue as it holds the most recent conversion rates
        // for a given currency pair
        val globalWindows = mutableMapOf<String, LinkedList<CurrencyConversionRate>>()

        // the maximum number of periods to keep track of for any alerter
        val maxWindow = alerters.maxBy { it.requiredPeriods }!!.requiredPeriods

        // keep track of the current start index for each alerter and currency pair
        // this is required because the global window map will contain the maximum number of periods across all
        // alerters, so an alerter that requires a smaller number of periods will need to only see a subset of this
        data class AlerterMeta(val currencyPair: String, val alerter: Alerter, var index: Int = 0)

        val alertWindowIndexes = mutableListOf<AlerterMeta>()

        rates.forEach {

            // add the current period to the global window for this currency pair
            val globalWindow = globalWindows.getOrPut(it.currencyPair) { LinkedList() }
            globalWindow.add(it)

            alerters.forEach { alerter ->

                if (globalWindow.size >= alerter.requiredPeriods) {
                    // enough periods to test this alert, get the start index

                    var alerterMeta = alertWindowIndexes.find { meta ->
                        meta.currencyPair == it.currencyPair
                                && meta.alerter.javaClass.name == alerter.javaClass.name
                    }

                    // new instance of alerter for each currency pair
                    if (alerterMeta == null) {
                        alerterMeta = AlerterMeta(it.currencyPair, alerter.clone())
                        alertWindowIndexes.add(alerterMeta)
                    }

                    // run the alerter
                    alerterMeta.alerter
                        .hasAlert(
                            it.currencyPair,
                            globalWindow.subList(
                                alerterMeta.index,
                                alerterMeta.index + alerter.requiredPeriods
                            )
                        )?.let { alert -> callback(alert) }

                    alerterMeta.index++
                }
            }

            // only keep track of the maximum periods for each currency pair required by any alerter, so
            // remove any old periods
            if (globalWindow.size > maxWindow) {
                alertWindowIndexes.forEach { meta ->
                    if (meta.currencyPair == it.currencyPair) {
                        meta.index--
                    }
                }
                globalWindow.remove()
            }
        }
    }

    /**
     * Called by the command line in App class.
     */
    fun start(rates: List<CurrencyConversionRate>) {

        val mapper = Mapper()

        process(
            rates, listOf(
                MovingAverageAlerter(300, 10.0), // 5 minute average, 10% threshold
                TrendingAlerter(900, 60) // 15 minutes, 1 minute throttle
            )
        ) { println(mapper.write(it)) }
    }
}
