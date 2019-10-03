package com.airwallex.codechallenge.input

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Stream

class Mapper {

    private val mapper = jacksonObjectMapper()
        .registerModule(JavaTimeModule())

    fun read(filename: String): Stream<CurrencyConversionRate> =
        read(Files.lines(Paths.get(filename)))

    fun read(lines: Stream<String>): Stream<CurrencyConversionRate> =
            lines.map {
                mapper.readValue<CurrencyConversionRate>(it)
            }

    fun write(alert: Alert) = mapper.writeValueAsString(alert)

    fun readMultiple(lines: Stream<String>): Stream<CurrencyConversionRate> {
        return lines.map {
            it.split('\n')
        }.map {
            it.map { line -> mapper.readValue<CurrencyConversionRate>(line) }
        }.flatMap { it.stream() }
    }
}
