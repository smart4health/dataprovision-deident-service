package com.healthmetrix.deident.persistence.dateshift.api

interface DateShiftRepository {
    operator fun set(patientId: String, shiftDays: Int)

    operator fun get(patientId: String): Int?
}
