package com.healthmetrix.deident.deidentifier

import ca.uhn.fhir.context.FhirContext
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterMethod
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterRule
import com.healthmetrix.deident.deidentifier.jsonfilter.JsonFilterSettings
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import java.io.InputStream
import com.github.michaelbull.result.runCatching as catch

@Configuration
class DeidentifierConfig {

    @Bean
    @Throws(Exception::class) // if parse fails or if no factory found for method
    fun provideJsonFilterRules(
        jsonFilterProperties: JsonFilterProperties,
        objectMapper: ObjectMapper,
        fhirContext: FhirContext,
        factories: Set<JsonFilterMethod.Factory>,
    ): List<JsonFilterRule> = with(fhirContext.newFhirPath()) {
        val rules = jsonFilterProperties.settings
            .asSequence()
            .map(::ClassPathResource)
            .map(Resource::getInputStream)
            .map<InputStream, JsonFilterSettings>(objectMapper::readValue)
            .fold(listOf<JsonFilterRule.Raw>()) { acc, el -> acc + el.rules }
            .map { raw ->
                val parseResult = catch {
                    parse(raw.path)
                }.mapError { it to raw.path }

                val factory = factories
                    .singleOrNull { it.name == raw.method }
                    ?.invoke(raw.params)
                    ?.let { JsonFilterRule(raw.path, it) }
                    .toResultOr { UnknownMethod(raw.method) }

                if (parseResult is Err) {
                    logger.error("Failed to parse ${parseResult.error.second} ${parseResult.error.first.message}")
                }

                if (factory is Err) {
                    logger.error("Unknown method '${factory.error.methodName}'")
                }

                if (parseResult is Ok && factory is Ok) {
                    factory.value
                } else {
                    null
                }
            }

        if (rules.any { it == null }) {
            error("Failed to create filter settings")
        }

        rules.filterNotNull()
    }

    data class UnknownMethod(val methodName: String)

    /**
     * List of files in the deidentifier resources directory to combine
     *
     * Order in the list matters, as rules are applied in order
     */
    @ConfigurationProperties(prefix = "json-filter")
    data class JsonFilterProperties(
        val settings: List<String>,
    )
}
