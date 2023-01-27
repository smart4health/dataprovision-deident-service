package com.healthmetrix.deident.persistence.job.api

import com.healthmetrix.deident.commons.JobId
import java.time.Instant

enum class JobStatus {
    Downloaded,
    TypeFiltered,
    PatientDeduplicated,
    Contextualized,
    DateShifted,
    Harmonized,
    Deidentified,
    Uploaded,
    Error,
}

data class Job(
    val id: JobId,
    val userId: String,
    val status: JobStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
)

interface JobRepository {
    fun save(job: Job)

    fun findJobById(jobId: JobId): Job?

    fun findJobsByUserId(userId: String): List<Job>

    fun countAllUsers(): Int
}
