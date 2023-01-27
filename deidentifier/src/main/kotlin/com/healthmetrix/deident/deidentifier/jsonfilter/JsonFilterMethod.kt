package com.healthmetrix.deident.deidentifier.jsonfilter

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import org.hl7.fhir.instance.model.api.IBase

/**
 * For every resource and every rule,
 * If the path matches an [IBase] of the resource,
 * Replace that [IBase] with the output of [accept]
 * If it is not Err(Ignored)
 */
interface JsonFilterMethod {
    /**
     * @return the IBase (or nothing) to replace the given IBase
     *         from the top level resource
     */
    fun accept(obj: IBase): Result<IBase?, Ignored>

    /**
     * Inject a Factory for each Method implementation
     *
     * Factories will be invoked based on whether the name
     * matches the [JsonFilterRule.Raw.method]
     *
     * Recommended to accept the object mapper as a constructor argument,
     * and then to construct methods with a single Params object, defined
     * per method, parsed using [ObjectMapper.treeToValue]
     */
    interface Factory {
        val name: String
        val objectMapper: ObjectMapper

        /**
         * @throws Exception if anything goes wrong, halting the spring initialization
         */
        operator fun invoke(params: JsonNode?): JsonFilterMethod
    }
}

object Ignored
