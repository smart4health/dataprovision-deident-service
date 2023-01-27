package com.healthmetrix.deident.debug.cache

import com.healthmetrix.deident.commons.HarmonizedEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Takes HarmonizedEvents as it's the last one before being uploaded to the research platform.
 * This should only be active on test environments
 */
@Profile("debug-cache & !production & !prod")
@Component
class DebugCacheInserter(
    private val inMemoryDebugCache: InMemoryDebugCache,
) {
    @EventListener
    fun onEvent(harmonizedEvent: HarmonizedEvent) {
        inMemoryDebugCache.put(
            InMemoryDebugCache.CacheItem(
                context = harmonizedEvent.context,
                fhirBundle = harmonizedEvent.transaction.asBundle(),
            ),
        )
    }
}
