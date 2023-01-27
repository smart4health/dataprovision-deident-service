package com.healthmetrix.deident.deidentifier.jsonfilter.method

import com.github.michaelbull.result.unwrap
import com.healthmetrix.deident.commons.replace
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.Address
import org.hl7.fhir.r4.model.Location
import org.junit.jupiter.api.Test

class RedactZipMethodTest {
    private val underTest = RedactZipMethod()

    @Test
    fun `zipRedacting shortens a zip code to 4 characters`() {
        val location = Location().apply {
            address = Address().apply {
                postalCode = "90210"
            }
        }

        val postalCode = location.address.postalCodeElement

        location.replace(postalCode, underTest.accept(postalCode).unwrap())

        assertThat(location.address.postalCode).isEqualTo("9021")
    }

    @Test
    fun `zipRedacting a zip code with length less than 5 does not change it`() {
        val location = Location().apply {
            address = Address().apply {
                postalCode = "1"
            }
        }

        val postalCode = location.address.postalCodeElement

        location.replace(postalCode, underTest.accept(postalCode).unwrap())

        assertThat(location.address.postalCode).isEqualTo("1")
    }
}
