package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.commons.ok
import com.healthmetrix.deident.deidentifier.jsonfilter.Ignored
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.StringType
import org.springframework.stereotype.Component

/**
 * Example method to demonstrate parameters
 */
class ReplaceMethod(
    private val params: Params,
) : JsonFilterMethod {

    override fun accept(obj: IBase): Result<IBase?, Ignored> = StringType(params.with).ok()

    data class Params(val with: String)

    @Component
    class Factory(override val objectMapper: ObjectMapper) : JsonFilterMethod.Factory {
        override val name = "replace"

        override fun invoke(params: JsonNode?): JsonFilterMethod = ReplaceMethod(objectMapper.treeToValue(params!!))
    }
}
