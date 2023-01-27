package com.healthmetrix.deident.dateshifter

import com.healthmetrix.deident.commons.ContextualizedEvent
import com.healthmetrix.deident.commons.DateShiftedEvent
import com.healthmetrix.deident.commons.IngestionContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.Transaction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import java.time.ZonedDateTime

class DateShifterTest {

    private val mockGetShiftDaysUseCase: GetShiftDaysUseCase = mockk {
        every { this@mockk.invoke(any()) } returns 7
    }

    private val mockShiftDateUseCase: ShiftDateUseCase = mockk()

    private val mockRejectResourceUseCase: RejectResourceUseCase = mockk {
        every { this@mockk.invoke(any(), any(), any()) } returns Unit
    }

    private val fakePatient = Patient()
    private val fakeObservation = Observation()

    private val mockApplicationEventPublisher: ApplicationEventPublisher = mockk {
        every { this@mockk.publishEvent(any<ApplicationEvent>()) } returns Unit
        every { this@mockk.publishEvent(any<Any>()) } returns Unit
    }

    private val fakeContextualizedEvent = ContextualizedEvent(
        context = IngestionContext(
            userId = "userId",
            jobId = JobId.randomUUID(),
            fetchedAt = ZonedDateTime.now(),
            d4lId = "d4lId",
        ),
        transaction = Transaction().update(fakePatient).create(fakeObservation),
    )

    private val underTest = DateShifter(
        getShiftDaysUseCase = mockGetShiftDaysUseCase,
        shiftDateUseCase = mockShiftDateUseCase,
        rejectResourceUseCase = mockRejectResourceUseCase,
        applicationEventPublisher = mockApplicationEventPublisher,
    )

    @Test
    fun `keep outcomes do not change the transaction`() {
        every { mockShiftDateUseCase.invoke(any(), any()) } returns ShiftDateUseCase.Outcome.KEEP
        underTest.onEvent(fakeContextualizedEvent)

        verify {
            mockApplicationEventPublisher.publishEvent(
                match<DateShiftedEvent> {
                    it.transaction == fakeContextualizedEvent.transaction
                },
            )
        }
    }

    @Test
    fun `truncate start outcomes remove the patient from the transaction`() {
        every {
            mockShiftDateUseCase.invoke(match<Patient> { true }, any())
        } returns ShiftDateUseCase.Outcome.TRUNCATE_START

        every {
            mockShiftDateUseCase.invoke(match<Observation> { true }, any())
        } returns ShiftDateUseCase.Outcome.KEEP

        underTest.onEvent(fakeContextualizedEvent)

        verify {
            mockApplicationEventPublisher.publishEvent(
                match<DateShiftedEvent> {
                    it.transaction.all == listOf(fakeObservation)
                },
            )
        }
    }

    @Test
    fun `truncate end outcomes remove the patient from the transaction`() {
        every {
            mockShiftDateUseCase.invoke(match<Patient> { true }, any())
        } returns ShiftDateUseCase.Outcome.TRUNCATE_END

        every {
            mockShiftDateUseCase.invoke(match<Observation> { true }, any())
        } returns ShiftDateUseCase.Outcome.KEEP

        underTest.onEvent(fakeContextualizedEvent)

        verify {
            mockApplicationEventPublisher.publishEvent(
                match<DateShiftedEvent> {
                    it.transaction.all == listOf(fakeObservation)
                },
            )
        }
    }
}
