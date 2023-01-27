package com.healthmetrix.deident.persistence.dateshift

import com.healthmetrix.deident.persistence.dateshift.api.DateShiftRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!rds & !postgres")
class InMemoryDateShiftRepository : DateShiftRepository {

    private val map = mutableMapOf<String, Int>()

    override fun set(patientId: String, shiftDays: Int) {
        map[patientId] = shiftDays
    }

    override fun get(patientId: String): Int? = map[patientId]
}
