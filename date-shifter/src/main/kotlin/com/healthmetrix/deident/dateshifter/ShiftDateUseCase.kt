package com.healthmetrix.deident.dateshifter

import ca.uhn.fhir.fhirpath.IFhirPath
import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.hl7.fhir.r4.model.Resource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.Date
import java.util.TimeZone

/**
 * Implementation of Shift And Truncate
 * https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5070517/
 *
 * @param cutoffStart a datetime, which drops all resources with dates/times before that moment
 * @param cutoffEnd a datetime, which drops all resources with dates/times after that moment.
 *                  changes over time, so it is a function
 */
@Component
class ShiftDateUseCase(
    @Qualifier("dateShiftPaths")
    private val paths: List<String>,
    private val fhirPath: IFhirPath,
    @Qualifier("cutoffStart")
    private val cutoffStart: ZonedDateTime,
    @Qualifier("cutoffEnd")
    private val cutoffEnd: () -> ZonedDateTime,
) {

    // shiftDays is a candidate for inline classes when they are ready
    operator fun invoke(resource: Resource, shiftDays: Int): Outcome {
        val zonedDateTimes = paths.flatMap { path ->
            fhirPath.evaluate(resource, path, BaseDateTimeType::class.java).mapNotNull { dateTime ->

                // if the precision is year we can hardly shift if by a number of days
                if (dateTime.precision == TemporalPrecisionEnum.YEAR) {
                    return@mapNotNull null
                }

                val newDate = dateTime.toZonedDateTime()
                    .plusDays(shiftDays.toLong())
                    .toInstant()
                    .let(Date::from)

                dateTime.setValue(newDate, dateTime.precision)

                dateTime.toZonedDateTime()
            }
        }

        val hasBefore = zonedDateTimes.any {
            it.isBefore(cutoffStart)
        }

        val hasAfter = zonedDateTimes.any {
            it.isAfter(cutoffEnd())
        }

        // prioritize TRUNCATE_START because it will never change, whereas
        // TRUNCATE_END will be retried in the future
        return when {
            hasBefore -> Outcome.TRUNCATE_START
            hasAfter -> Outcome.TRUNCATE_END
            else -> Outcome.KEEP
        }
    }

    /**
     * BaseDateTimeType, as a PrimitiveType<Date>, is a moment in time,
     * equivalent to an Instant in modern java
     *
     * When representing DateTypes, for example, it will parse it as
     * midnight of the __default timezone__ but not set that on the
     * object itself
     *
     * So, always use a ZonedDateTime in modern java to accurately
     * represent the parsed value, even if it doesn't semantically
     * match up with FHIR
     */
    private fun BaseDateTimeType.toZonedDateTime(): ZonedDateTime {
        val zoneId = when {
            timeZone != null -> timeZone.toZoneId()
            else -> TimeZone.getDefault().toZoneId()
        }

        return ZonedDateTime.ofInstant(value.toInstant(), zoneId)
    }

    enum class Outcome {
        KEEP,
        TRUNCATE_START,
        TRUNCATE_END,
    }
}
