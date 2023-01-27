package com.healthmetrix.deident.harmonizer.harmonize

import com.healthmetrix.deident.harmonizer.qomopclient.HarmonizationResponse
import com.healthmetrix.deident.harmonizer.qomopclient.QomopCoding
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Extension
import org.hl7.fhir.r4.model.StringType

const val HARMONIZATION_RESULT_URL = "/harmonization/extension/result"
const val HARMONIZATION_ORIGINAL_CODING_URL = "/harmonization/extension/original"

/**
 * Maps the harmonization result to the FHIR [org.hl7.fhir.r4.model.Coding] item to be replaced.
 * On all Codings one [org.hl7.fhir.r4.model.Extension] item is added with the HarmonizationResult status
 * On all Codings with changed value, the original one is added as separate extension.
 */
fun HarmonizationResponse.toNewCoding(original: Coding): Coding {
    val targetCoding = harmonizedCoding?.toFhirCoding() ?: original
    val extensions = mutableListOf(Extension(HARMONIZATION_RESULT_URL, StringType(message)))

    // Comparing original with Harmonized
    if (!targetCoding.equalsDeep(original)) {
        extensions.add(Extension(HARMONIZATION_ORIGINAL_CODING_URL, original))
    }

    extensions.forEach { targetCoding.addExtension(it) }
    return targetCoding
}

private fun QomopCoding.toFhirCoding(): Coding = Coding().apply {
    system = this@toFhirCoding.system
    code = this@toFhirCoding.code
    display = this@toFhirCoding.display
}
