package com.healthmetrix.deident.typefilter

import com.healthmetrix.deident.commons.DownloadedEvent
import com.healthmetrix.deident.commons.IngestionContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.Transaction
import com.healthmetrix.deident.commons.TypeFilteredEvent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hl7.fhir.r4.model.AllergyIntolerance
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.Consent
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Immunization
import org.hl7.fhir.r4.model.MedicationStatement
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Procedure
import org.hl7.fhir.r4.model.Provenance
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.ZonedDateTime

class TypeFilterTest {

    private val mockTypeFilterRejectResourceUseCase = mockk<TypeFilterRejectResourceUseCase> {
        every { this@mockk.invoke(any(), any()) } returns Unit
    }

    private val mockApplicationEventPublisher = mockk<ApplicationEventPublisher> {
        every { this@mockk.publishEvent(any<ApplicationEvent>()) } returns Unit
        every { this@mockk.publishEvent(any<Any>()) } returns Unit
    }

    private val underTest = TypeFilter(
        typeFilterRejectResourceUseCase = mockTypeFilterRejectResourceUseCase,
        applicationEventPublisher = mockApplicationEventPublisher,
    )

    private val fakeContext = IngestionContext(
        userId = "userId",
        jobId = JobId.randomUUID(),
        fetchedAt = ZonedDateTime.now(),
        d4lId = "d4lId",
    )

    private val fakeListAllAcceptedResource = listOf(
        AllergyIntolerance(),
        Condition(),
        Consent(),
        Encounter(),
        Immunization(),
        MedicationStatement(),
        Observation(),
        Patient(),
        Procedure(),
        QuestionnaireResponse(),
    )

    @Test
    fun `all accepted resource types remain in the transaction`() {
        val fakeTransaction = Transaction(
            toUpdate = fakeListAllAcceptedResource,
            toCreate = fakeListAllAcceptedResource,
        )

        underTest.onEvent(DownloadedEvent(fakeContext, fakeTransaction))

        verify {
            mockApplicationEventPublisher.publishEvent(
                match<TypeFilteredEvent> { event ->
                    event.transaction.toUpdate == fakeListAllAcceptedResource &&
                        event.transaction.toCreate == fakeListAllAcceptedResource
                },
            )
        }
    }

    @Test
    fun `non accepted resources are removed from the transaction`() {
        val fakeTransaction = Transaction(
            toUpdate = fakeListAllAcceptedResource.plus(Provenance()),
            toCreate = fakeListAllAcceptedResource.plus(DocumentReference()),
        )

        underTest.onEvent(DownloadedEvent(fakeContext, fakeTransaction))

        verify {
            mockApplicationEventPublisher.publishEvent(
                match<TypeFilteredEvent> { event ->
                    event.transaction.toUpdate == fakeListAllAcceptedResource &&
                        event.transaction.toCreate == fakeListAllAcceptedResource
                },
            )
        }
    }

    @Test
    fun `all resources are removed from the transaction if all are not accepted`() {
        val fakeTransaction = Transaction(
            toUpdate = listOf(DocumentReference()),
            toCreate = listOf(Provenance()),
        )

        underTest.onEvent(DownloadedEvent(fakeContext, fakeTransaction))

        verify {
            mockApplicationEventPublisher.publishEvent(
                match<TypeFilteredEvent> { event ->
                    event.transaction.all.isEmpty()
                },
            )
        }
    }
}
