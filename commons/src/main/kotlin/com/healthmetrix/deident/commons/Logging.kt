package com.healthmetrix.deident.commons

import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val Any.logger: Logger
    get() = LoggerFactory.getLogger(javaClass)

infix fun String.kv(other: Any?): StructuredArgument =
    StructuredArguments.kv(this, other)
