package com.healthmetrix.deident.deidentifier.jsonfilter

import ca.uhn.fhir.fhirpath.IFhirPath
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.commons.replace
import com.healthmetrix.deident.deidentifier.prune
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Resource
import org.springframework.stereotype.Component

@Component
class JsonFilterUseCase(
    private val fhirPath: IFhirPath,
    private val rules: List<JsonFilterRule>,
) {
    /**
     * Mutate the given resource based on pre configured JsonFilterSettings
     */
    operator fun invoke(resource: Resource, d4lId: String?): Result<Unit, Throwable> = binding {
        val liveSet = mutableSetOf<IBase>()
        rules.forEach { rule ->
            fhirPath.evaluate(resource, rule.path, IBase::class.java).forEach { found ->
                rule.method.accept(found)
                    .onSuccess { transformed ->
                        // if the method does not mutate, replace iBase using reflection
                        if (transformed !== found) {
                            resource.replace(found, transformed).bind()
                        }

                        transformed?.let(liveSet::add)
                    }
                    .onFailure {
                        logger.warn("JsonFilterMethod ${rule.method::class.simpleName} does not accept ${found::class.java}")
                        return@binding
                    }
            }
        }

        resource.prune(liveSet)
    }
}
