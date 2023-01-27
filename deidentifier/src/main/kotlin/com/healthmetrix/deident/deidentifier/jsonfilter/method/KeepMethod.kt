package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.commons.ok
import com.healthmetrix.deident.deidentifier.jsonfilter.Ignored
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import org.hl7.fhir.instance.model.api.IBase
import org.springframework.stereotype.Component

class KeepMethod : JsonFilterMethod {
    override fun accept(obj: IBase): Result<IBase?, Ignored> = obj.ok()

    @Component
    class Factory(override val objectMapper: ObjectMapper) : JsonFilterMethod.Factory {
        override val name = "keep"

        override fun invoke(params: JsonNode?): JsonFilterMethod = KeepMethod()
    }
}
