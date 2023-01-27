package com.healthmetrix.deident.debug.cache

import com.google.common.collect.EvictingQueue
import com.healthmetrix.deident.commons.IngestionContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.logger
import org.hl7.fhir.r4.model.Bundle
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Suppress("UnstableApiUsage")
@Profile("debug-cache & !production & !prod")
@Component
class InMemoryDebugCache(
    @Value("\${debug-cache-max-size}")
    private val maxSize: Int,
) {

    private val evictingCache: EvictingQueue<CacheItem> = EvictingQueue.create(maxSize)

    fun put(item: CacheItem) {
        logger.info("[debug cache] storing item for jobId={${item.context.jobId}}, userId={${item.context.userId}} in debug cache.")
        evictingCache += item
    }

    fun get(jobId: JobId): CacheItem? = evictingCache.firstOrNull { it.context.jobId == jobId }

    fun getLast(): CacheItem? = evictingCache.lastOrNull()

    fun findByUserId(userId: String): List<CacheItem> = evictingCache.filter { it.context.userId == userId }

    fun clear() {
        evictingCache.clear()
    }

    data class CacheItem(
        val context: IngestionContext,
        val fhirBundle: Bundle,
    )
}
