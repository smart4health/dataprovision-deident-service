package com.healthmetrix.deident.buildlogic.localdriver

import ca.uhn.fhir.context.FhirContext
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

open class FhirXmlToJsonTask : DefaultTask() {

    @Input
    @set:Option(option = "input", description = "filename to read xml from (relative to project root), or - for stdin")
    var inputFilename: String = "build-logic/local-driver/src/main/resources/xml-to-json/patient.xml"

    @Input
    @set:Option(option = "compact", description = "If compact is set, the json will not be pretty printed")
    var compact: Boolean = false

    @Input
    @set:Option(
        option = "output",
        description = "filname to write json to (relative to project root), or - for stdout.  Will not overwrite",
    )
    var outputFilename: String = "build-logic/local-driver/src/main/resources/xml-to-json/patient.json"

    @TaskAction
    fun taskAction() {
        val inputStream = when (inputFilename) {
            "-" -> System.`in`
            else -> project.file(inputFilename).inputStream()
        }

        val fhirContext = FhirContext.forR4()

        val parsed = try {
            fhirContext.newXmlParser().parseResource(inputStream)
        } catch (ex: Exception) {
            throw GradleException("Failed to parse xml", ex)
        }

        val outputStream = when (outputFilename) {
            "-" -> System.out
            else -> project.file(outputFilename).outputStream()
        }

        fhirContext.newJsonParser()
            .apply { setPrettyPrint(!compact) }
            .encodeResourceToWriter(parsed, outputStream.writer())
    }
}
