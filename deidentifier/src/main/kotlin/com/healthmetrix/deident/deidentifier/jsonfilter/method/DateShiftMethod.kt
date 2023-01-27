package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.commons.err
import com.healthmetrix.deident.commons.ok
import com.healthmetrix.deident.deidentifier.jsonfilter.Ignored
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.BaseDateTimeType
import org.springframework.stereotype.Component

class DateShiftMethod : JsonFilterMethod {
    /**
     * Handled previously by the date-shifter pipeline step, so
     * this just becomes equivalent to keep
     */
    override fun accept(obj: IBase): Result<IBase?, Ignored> = when (obj) {
        is BaseDateTimeType -> obj.ok()
        else -> Ignored.err()
    }

    @Component
    class Factory(override val objectMapper: ObjectMapper) : JsonFilterMethod.Factory {
        override val name = "dateShift"

        override fun invoke(params: JsonNode?): JsonFilterMethod = DateShiftMethod()
    }
}
