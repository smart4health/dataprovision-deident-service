package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.commons.err
import com.healthmetrix.deident.commons.ok
import com.healthmetrix.deident.deidentifier.jsonfilter.Ignored
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.StringType
import org.springframework.stereotype.Component

class RedactZipMethod : JsonFilterMethod {
    /**
     * Keep the first four digits and redact the rest
     *
     * TODO may be country dependent
     */
    override fun accept(obj: IBase): Result<IBase?, Ignored> {
        if (obj !is StringType) {
            return Ignored.err()
        }

        return obj.valueNotNull.take(4).let(::StringType).ok()
    }

    @Component
    class Factory(override val objectMapper: ObjectMapper) : JsonFilterMethod.Factory {
        override val name = "redactZip"

        override fun invoke(params: JsonNode?): JsonFilterMethod = RedactZipMethod()
    }
}
