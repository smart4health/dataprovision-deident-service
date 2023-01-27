package com.healthmetrix.deident.buildlogic.localdriver

import ca.uhn.fhir.context.FhirContext
import com.opencsv.CSVReaderBuilder
import com.opencsv.bean.CsvBindByName
import com.opencsv.bean.CsvToBeanBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.hl7.fhir.r4.model.Bundle
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Dosage
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Quantity
import org.hl7.fhir.r4.model.Ratio
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Timing
import java.io.InputStreamReader
import java.math.BigDecimal
import java.util.UUID

private const val SNOMED_URL = "http://snomed.info/sct"
private const val ATC_URL = "http://www.whocc.no/atc"
private const val MUMC_URL = "mumc://our-system"

/**
 * This task takes a medication dataset in a custom format by MUMC+ and wraps it to a bundle containing
 * a random Patient and for each CSV line one MedicationStatement resource with some values. The format
 * of this resource is based on the MedicationStatement-template.json
 * Output files will override previous ones and be pretty printed
 */
open class FhirMumcCsvToJsonTask : DefaultTask() {

    @Input
    @set:Option(option = "input", description = "filename to read csv from (relative to project root)")
    var inputFilename: String = "build-logic/local-driver/src/main/resources/mumc-to-json/Medications.csv"

    @Input
    @set:Option(
        option = "input",
        description = "template for a MedicationStatement with some common fields (relative to project root)",
    )
    var templateFilename: String = "build-logic/local-driver/src/main/resources/mumc-to-json/MedicationStatement-template.json"

    @Input
    @set:Option(
        option = "output",
        description = "filename to write json to (relative to project root)",
    )
    var outputFilename: String = "build-logic/local-driver/src/main/resources/mumc-to-json/mumc.medication.bundle.json"

    @TaskAction
    fun taskAction() {
        val fhirContext = FhirContext.forR4()
        val jsonParser = fhirContext.newJsonParser()

        val medStatementInfos = try {
            InputStreamReader(project.file(inputFilename).inputStream())
                .use {
                    CsvToBeanBuilder<MumcMedicationItem>(CSVReaderBuilder(it).build())
                        .withType(MumcMedicationItem::class.java)
                        .withIgnoreLeadingWhiteSpace(true)
                        .build()
                        .parse()
                }
        } catch (ex: Exception) {
            throw GradleException("Failed to parse input CSV", ex)
        }
        val template = try {
            jsonParser.parseResource(project.file(templateFilename).inputStream()) as MedicationStatement
        } catch (ex: Exception) {
            throw GradleException("Failed to parse MedicationStatement template", ex)
        }

        val patient = Patient().apply { id = UUID.randomUUID().toString() }

        val resultBundle = Bundle().apply {
            type = Bundle.BundleType.COLLECTION
            addEntry().also { entry ->
                entry.resource = patient
            }
            medStatementInfos.forEach {
                addEntry().also { entry ->
                    entry.resource = buildResourceFromItem(template, it, patient)
                }
            }
        }

        jsonParser
            .apply { setPrettyPrint(true) }
            .encodeResourceToWriter(resultBundle, project.file(outputFilename).outputStream().writer())
    }
}

private fun buildResourceFromItem(
    template: MedicationStatement,
    medItem: MumcMedicationItem,
    patient: Patient,
): MedicationStatement {
    return template.copy().apply {
        id = UUID.randomUUID().toString()
        medication =
            CodeableConcept().apply {
                coding = listOf(
                    Coding(MUMC_URL, medItem.medicationId, medItem.description),
                    Coding().apply { system = ATC_URL; code = medItem.atcColumn },
                )
            }
        dosage = listOf(
            Dosage().apply {
                route = medItem.route.toCoding()?.let { CodeableConcept(it) }
                text = medItem.instructions
                maxDosePerPeriod = medItem.dosage.toMaxDosagePerPeriod()
                timing = medItem.dosage.toTiming() ?: Timing().apply {
                    code = CodeableConcept().apply { text = medItem.dosage }
                }
            },
        )
        subject = Reference(patient)
    }
}

private fun String.toCoding(): Coding? {
    return when (this) {
        "Inhalatie" -> Coding(SNOMED_URL, "420641004", "Solution for inhalation")
        "Subcutaan" -> Coding(SNOMED_URL, "263887005", "Subcutaneous")
        "Intrathecaal" -> Coding(SNOMED_URL, "420887008", "Intrathecal suspension")
        "Intraveneus" -> Coding(SNOMED_URL, "421410002", "Intravenous solution")
        "Oculair" -> Coding(SNOMED_URL, "421987002", "Intraocular solution")
        "Rectaal" -> Coding(SNOMED_URL, "420929008", "Rectal suppository")
        "Cutaan" -> Coding(SNOMED_URL, "422201009", "Tablet for cutaneous solution")
        "Oraal" -> Coding(SNOMED_URL, "421026006", "Oral tablet")
        else -> null
    }
}

data class MumcMedicationItem(
    @CsvBindByName(column = "Med_Description_1", required = true)
    val description: String = "",
    @CsvBindByName(column = "Med_ATC", required = true)
    val atcColumn: String = "",
    @CsvBindByName(column = "MedicationID", required = true)
    val medicationId: String = "",
    @CsvBindByName(column = "Med_Dosage", required = true)
    val dosage: String = "",
    @CsvBindByName(column = "Med_Route", required = true)
    val route: String = "",
    @CsvBindByName(column = "Med_Instructions")
    val instructions: String? = null,
)

private fun String.toTiming(): Timing? {
    val matchEntire = Regex(pattern = "(\\d)D").find(this)
    return when (matchEntire?.groupValues?.size) {
        2 -> Timing().apply {
            repeat = Timing.TimingRepeatComponent()
                .apply {
                    frequency = matchEntire.groupValues[1].toInt(); periodUnit = Timing.UnitsOfTime.D; period =
                        BigDecimal.ONE
                }
        }
        else -> null
    }
}

private fun String.toMaxDosagePerPeriod(): Ratio? {
    val matchEntire = Regex(pattern = "\\((\\d)\\)").find(this)
    return when (matchEntire?.groupValues?.size) {
        2 -> Ratio().apply {
            numerator = Quantity(matchEntire.groupValues[1].toLong()); denominator = Quantity(1)
        }
        else -> null
    }
}
