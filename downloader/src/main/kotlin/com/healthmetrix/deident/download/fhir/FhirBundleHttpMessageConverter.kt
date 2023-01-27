package com.healthmetrix.deident.download.fhir

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import org.hl7.fhir.r4.model.Bundle
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.stereotype.Component

const val APPLICATION_FHIR_JSON_VALUE = "application/fhir+json"
val APPLICATION_FHIR_JSON = MediaType.valueOf(APPLICATION_FHIR_JSON_VALUE)

@Component
class FhirBundleHttpMessageConverter(
    private val fhirContext: FhirContext,
) : HttpMessageConverter<Bundle> {
    override fun getSupportedMediaTypes(): MutableList<MediaType> {
        return mutableListOf(APPLICATION_FHIR_JSON)
    }

    override fun write(t: Bundle, contentType: MediaType?, outputMessage: HttpOutputMessage) {
        val s = try {
            fhirContext.newJsonParser().encodeResourceToString(t)
        } catch (ex: DataFormatException) {
            throw HttpMessageNotWritableException(ex.message ?: "failed to write bundle to string")
        }
        outputMessage.body.write(s.toByteArray())
    }

    override fun read(clazz: Class<out Bundle>, inputMessage: HttpInputMessage): Bundle = try {
        fhirContext.newJsonParser().parseResource(Bundle::class.java, inputMessage.body)
    } catch (ex: DataFormatException) {
        throw HttpMessageNotReadableException(ex.message ?: "failed to read bundle from string", inputMessage)
    }

    override fun canRead(clazz: Class<*>, mediaType: MediaType?): Boolean {
        return clazz.isAssignableFrom(Bundle::class.java) && APPLICATION_FHIR_JSON.equalsTypeAndSubtype(mediaType)
    }

    override fun canWrite(clazz: Class<*>, mediaType: MediaType?): Boolean {
        return clazz.isAssignableFrom(Bundle::class.java) && APPLICATION_FHIR_JSON.equalsTypeAndSubtype(mediaType)
    }
}
