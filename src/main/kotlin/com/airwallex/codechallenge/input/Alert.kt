package com.airwallex.codechallenge.input

import java.time.Instant

data class Alert (
    val timestamp: Instant,
    val currencyPair: String,
    val alert: String,
    val seconds: Long? = null
)