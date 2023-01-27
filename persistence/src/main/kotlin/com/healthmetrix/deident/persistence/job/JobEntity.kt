package com.healthmetrix.deident.persistence.job

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.JobStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.sql.Timestamp

@Entity
@Table(name = "job")
data class JobEntity(
    @Id
    val id: JobId,

    @Column(name = "user_id")
    val userId: String,

    @Enumerated(EnumType.STRING)
    val status: JobStatus,

    @Column(name = "created_at")
    val createdAt: Timestamp,

    @Column(name = "updated_at")
    val updatedAt: Timestamp,
)
