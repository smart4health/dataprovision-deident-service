package com.healthmetrix.deident.harmonizer

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.IFhirPath
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class HarmonizerAcceptanceConfiguration {
    @Bean
    fun provideFhirContext(): FhirContext = FhirContext.forR4()

    @Bean
    fun provideFhirPath(fhirContext: FhirContext): IFhirPath = fhirContext.newFhirPath()
}
