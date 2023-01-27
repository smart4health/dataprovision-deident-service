package com.healthmetrix.deident.persistence.job

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Component
import java.sql.Timestamp

@Component
@Profile("rds | postgres")
class RemoteJobRepository(
    private val remoteJobCrudRepository: RemoteJobCrudRepository,
) : JobRepository {

    override fun save(job: Job) {
        remoteJobCrudRepository.save(
            JobEntity(
                id = job.id,
                userId = job.userId,
                status = job.status,
                createdAt = job.createdAt.toEpochMilli().let(::Timestamp),
                updatedAt = job.updatedAt.toEpochMilli().let(::Timestamp),
            ),
        )
    }

    override fun findJobById(jobId: JobId): Job? {
        return remoteJobCrudRepository.findById(jobId).map(JobEntity::toJob).orElse(null)
    }

    override fun findJobsByUserId(userId: String): List<Job> {
        return remoteJobCrudRepository.findByUserId(userId).map(JobEntity::toJob)
    }

    override fun countAllUsers(): Int = remoteJobCrudRepository.countAllUsers()
}

@Profile("rds | postgres")
interface RemoteJobCrudRepository : CrudRepository<JobEntity, JobId> {

    @Query("SELECT j FROM JobEntity j WHERE j.userId = :userId")
    fun findByUserId(userId: String): List<JobEntity>

    @Query("SELECT COUNT(DISTINCT j.userId) FROM JobEntity j")
    fun countAllUsers(): Int
}

private fun JobEntity.toJob(): Job {
    return Job(
        id = id,
        userId = userId,
        status = status,
        createdAt = createdAt.toInstant(),
        updatedAt = updatedAt.toInstant(),
    )
}
