package com.healthmetrix.deident.persistence.dateshift

import com.healthmetrix.deident.persistence.dateshift.api.DateShiftRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component

@Component
@Profile("rds | postgres")
class RemoteDateShiftRepository(
    private val remoteDateShiftCrudRepository: RemoteDateShiftCrudRepository,
) : DateShiftRepository {
    override fun set(patientId: String, shiftDays: Int) {
        DateShiftEntity(patientId, shiftDays)
            .let(remoteDateShiftCrudRepository::save)
    }

    override fun get(patientId: String): Int? =
        remoteDateShiftCrudRepository
            .findById(patientId)
            .orElse(null)
            ?.shiftDays
}

@Profile("rds | postgres")
interface RemoteDateShiftCrudRepository : CrudRepository<DateShiftEntity, String>

@Entity
@Table(name = "date_shift")
data class DateShiftEntity(
    @Id
    @Column(name = "patient_id")
    val patientId: String,

    @Column(name = "shift_days")
    val shiftDays: Int,
)
