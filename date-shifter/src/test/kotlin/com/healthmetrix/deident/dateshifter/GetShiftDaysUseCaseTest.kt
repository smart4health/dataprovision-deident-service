package com.healthmetrix.deident.dateshifter

import com.healthmetrix.deident.persistence.dateshift.api.DateShiftRepository
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.security.SecureRandom

class GetShiftDaysUseCaseTest {

    private val dateShiftRepository: DateShiftRepository = mockk()
    private val secureRandom: SecureRandom = mockk()
    private val fixedDateShiftDays: Int? = null
    private val underTest = GetShiftDaysUseCase(dateShiftRepository, secureRandom, fixedDateShiftDays)
    private val patientId = "123456"

    @Test
    fun `getting an existing dateShift from the repository`() {
        every { dateShiftRepository[patientId] } returns 3
        assertThat(underTest(patientId)).isEqualTo(3)
    }

    @Test
    fun `generating a new random dateshift and storing it in the repository`() {
        val dateShiftDays = 10
        every { dateShiftRepository[patientId] } returns null
        every { dateShiftRepository[patientId] = any() } just runs
        every { secureRandom.nextInt(any()) } returns dateShiftDays - 1

        assertThat(underTest(patientId)).isEqualTo(dateShiftDays)
        verify { dateShiftRepository[patientId] = dateShiftDays }
    }

    @Test
    fun `overriding the random fixedDays with 5`() {
        GetShiftDaysUseCase(dateShiftRepository, secureRandom, 5).invoke(patientId).let {
            assertThat(it).isEqualTo(5)
        }
    }
}
