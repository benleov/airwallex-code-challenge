package com.airwallex.codechallenge.input

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.Instant

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Alert(
    val timestamp: Instant,
    val currencyPair: String,
    val alert: String,
    val seconds: Long? = null
)