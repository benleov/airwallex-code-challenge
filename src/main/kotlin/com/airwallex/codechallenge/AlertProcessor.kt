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
        data class AlerterMeta(val alerterIndex: Int, val currencyPair: String, val alerter: Alerter, var offset: Int = 0) {
          override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AlerterMeta
            if (alerter.javaClass != other.alerter.javaClass) return false
            if (alerterIndex != other.alerterIndex) return false
            if (currencyPair != other.currencyPair) return false
            return true
          }

          override fun hashCode(): Int {
            var result = alerterIndex
            result = 31 * result + currencyPair.hashCode()
            result = 1 * result + alerter.javaClass.hashCode()
            return result
          }
        }

        val runningAlerters = mutableListOf<AlerterMeta>()

        // as each pair is processed
        // put into its own list (global)
        // if the list has the required number of spot prices, run an alerter
        // if the list is greater than the required number of spot prices for any aleters, trim it

        rates.forEach {

            // add the current period to the global window for this currency pair
            val pairWindow = globalWindows.getOrPut(it.currencyPair) { LinkedList() }
            pairWindow.add(it)
            alerters.forEach { alerter ->

                if (pairWindow.size >= alerter.requiredPeriods) {

                    // enough periods to test this alert, get the start index
                    var alerterMeta = runningAlerters.find { meta -> meta == AlerterMeta(alerters.indexOf(alerter), it.currencyPair, alerter, 0) }
                    // new instance of alerter for each currency pair
                    if (alerterMeta == null) {
                        alerterMeta = AlerterMeta(alerters.indexOf(alerter), it.currencyPair, alerter.clone(), 0)
                      runningAlerters.add(alerterMeta)
                    }

                    alerterMeta.alerter
                      .hasAlert(
                        it.currencyPair,
                        pairWindow.subList(
                          alerterMeta.offset,
                          alerterMeta.offset + alerter.requiredPeriods
                        )
                      )?.let { alert -> callback(alert) }

                  alerterMeta.offset++

                }
            }

            // only keep track of the maximum periods for each currency pair required by any alerter, so
            // remove any old periods

            if (pairWindow.size > maxWindow) {

                // reduce offset
                runningAlerters.forEach { meta ->
                  // for each alerter window
                    if (meta.currencyPair == it.currencyPair) {
                        meta.offset--
                    }
                }
                pairWindow.remove()
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
