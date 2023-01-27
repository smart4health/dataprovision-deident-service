package com.healthmetrix.deident.deidentifier.jsonfilter

import com.fasterxml.jackson.databind.JsonNode

data class JsonFilterSettings(
    val rules: List<JsonFilterRule.Raw>,
)

data class JsonFilterRule(
    val path: String,
    val method: JsonFilterMethod,
) {
    data class Raw(
        val path: String,
        val method: String,
        val params: JsonNode?,
    )
}
