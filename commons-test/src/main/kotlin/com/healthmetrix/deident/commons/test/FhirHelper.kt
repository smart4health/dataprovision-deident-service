package com.healthmetrix.deident.commons.test

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.fhirpath.IFhirPath
import org.hl7.fhir.instance.model.api.IBase
import org.hl7.fhir.r4.model.Resource

class FhirHelper(
    val fhirContext: FhirContext = FhirContext.forR4(),
) {
    val fhirPath: IFhirPath = fhirContext.newFhirPath()

    /**
     * WARNING: encoding resource to string calls getIdElement, which auto-creates
     * to an empty-but-not-actually-null IdType... so it doesn't show up when encoding
     * but may throw off tests looking at actual child objects
     */
    fun json(resource: Resource): String =
        fhirContext.newJsonParser().encodeResourceToString(resource)

    fun path(resource: Resource, path: String): List<IBase> =
        fhirPath.evaluate(resource, path, IBase::class.java)

    // filePath relative to /resources
    inline fun <reified T : Resource> load(
        filePath: String = "${T::class.simpleName}.json",
    ): T = with(fhirContext.newJsonParser()) {
        javaClass.classLoader
            .getResource(filePath)!!
            .readText()
            .let { parseResource(T::class.java, it) }
    }
}
