package com.healthmetrix.deident.deidentifier.jsonfilter.method

import ca.uhn.fhir.model.api.TemporalPrecisionEnum
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.commons.err
import com.healthmetrix.deident.commons.ok
import com.healthmetrix.deident.deidentifier.jsonfilter.Ignored
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.springframework.stereotype.Component
import java.time.Year

class DateRedactMethod(
    private val currentYear: () -> Year,
) : JsonFilterMethod {
    /**
     * if age < 90, Keep only year,
     * if age >= 90, redact fully
     *
     * Note that DateType has a max precision of one day, and no timezone
     *
     * Note that this only has year-wise precision, with the assumption that
     * looking at fine-grained patient data over time, and noting when the
     * birthday disappears, would leak the birth date
     *
     * DateType uses old java time apis behind the scenes, so
     * day of months starts at 1 (https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html#DAY_OF_MONTH)
     * while months start at 0 (https://docs.oracle.com/javase/7/docs/api/java/util/Calendar.html#MONTH)
     */
    override fun accept(obj: IBase): Result<IBase?, Ignored> {
        val birthYear = when (obj) {
            is DateType -> obj.year
            is DateTimeType -> obj.year
            else -> return Ignored.err()
        }.let(Year::of)

        val cutOff = currentYear().minusYears(90)

        if (birthYear.isBefore(cutOff) || birthYear == cutOff) {
            return null.ok()
        }

        return DateType().apply {
            precision = TemporalPrecisionEnum.YEAR
            day = 1
            month = 0
            year = birthYear.value
        }.ok()
    }

    @Component
    class Factory(override val objectMapper: ObjectMapper) : JsonFilterMethod.Factory {
        override val name = "dateRedact"

        override fun invoke(params: JsonNode?): JsonFilterMethod = DateRedactMethod(Year::now)
    }
}
