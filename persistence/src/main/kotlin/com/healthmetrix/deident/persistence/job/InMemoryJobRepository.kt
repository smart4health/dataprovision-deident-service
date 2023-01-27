package com.healthmetrix.deident.persistence.job

import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!rds & !postgres")
class InMemoryJobRepository : JobRepository {

    private val cache: MutableMap<JobId, Job> = mutableMapOf()

    override fun save(job: Job) {
        cache[job.id] = job
    }

    override fun findJobById(jobId: JobId): Job? {
        return cache[jobId]
    }

    override fun findJobsByUserId(userId: String): List<Job> {
        return cache.values.filter { it.userId == userId }
    }

    override fun countAllUsers(): Int = cache.values.distinctBy { it.userId }.size
}
