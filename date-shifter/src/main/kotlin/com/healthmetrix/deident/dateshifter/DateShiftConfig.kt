package com.healthmetrix.deident.dateshifter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.deident.commons.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

@Configuration
class DateShiftConfig {
    @Bean("cutoffStart")
    fun provideCutoffStart(
        @Value("\${date-shift.cutoff-start}")
        day: String,
    ): ZonedDateTime = LocalDate.parse(day).atStartOfDay(ZoneId.of("UTC"))

    @Bean("fixedDateShiftDays")
    fun provideFixedDateShiftDays(
        @Value("\${date-shift.fixed-date-shift-days:#{null}}")
        days: String?,
    ): Int? =
        days?.toIntOrNull()?.also { logger.info("Override random amount of days for DateShifter with fixed value $it") }

    @Bean("cutoffEnd")
    fun provideCutoffEnd(): () -> ZonedDateTime = {
        LocalDate.now(Clock.systemUTC())
            .atStartOfDay(ZoneId.of("UTC"))
    }

    @Bean("dateShiftPaths")
    fun provideDateShiftPaths(
        @Value("classpath:date_shift_paths.json")
        pathsResource: Resource,
        objectMapper: ObjectMapper,
    ) = objectMapper.readValue<List<String>>(pathsResource.inputStream)
}
