package com.healthmetrix.deident.debug.cache

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.IFhirPath
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DebugCacheAcceptanceConfiguration {
    @Bean
    fun provideFhirContext(): FhirContext = FhirContext.forR4()

    @Bean
    fun provideFhirPath(fhirContext: FhirContext): IFhirPath = fhirContext.newFhirPath()
}
