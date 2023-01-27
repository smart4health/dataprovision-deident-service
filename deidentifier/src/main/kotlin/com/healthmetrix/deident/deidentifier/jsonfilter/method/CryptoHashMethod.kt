package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.healthmetrix.deident.commons.err
import com.healthmetrix.deident.commons.ok
import com.healthmetrix.deident.deidentifier.jsonfilter.Ignored
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Reference
import org.springframework.stereotype.Component

class CryptoHashMethod : JsonFilterMethod {
    /**
     * Cryptographically hashing identifiers is taken care of immediately
     * after downloading, in order to persist metadata.  As such, cryptoHash
     * rules in the ruleset are equivalent to keeps
     */
    override fun accept(obj: IBase): Result<IBase?, Ignored> = when (obj) {
        is Reference, is Identifier, is IdType -> obj.ok()
        else -> Ignored.err()
    }

    @Component
    class Factory(override val objectMapper: ObjectMapper) : JsonFilterMethod.Factory {
        override val name = "cryptoHash"

        override fun invoke(params: JsonNode?): JsonFilterMethod = CryptoHashMethod()
    }
}
