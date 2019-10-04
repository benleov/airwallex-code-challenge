package com.airwallex.codechallenge.alerters

import com.airwallex.codechallenge.input.Alert
import com.airwallex.codechallenge.input.CurrencyConversionRate

abstract class Alerter(val requiredPeriods: Int) {

    /**
     * Returns an alert if one is generated over the provided period for this currency pair.
     */
    abstract fun hasAlert(currencyPair: String, rates: List<CurrencyConversionRate>): Alert?

    abstract fun clone(): Alerter
}