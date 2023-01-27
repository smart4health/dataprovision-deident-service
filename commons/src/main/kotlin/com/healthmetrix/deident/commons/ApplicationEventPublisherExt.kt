package com.healthmetrix.deident.commons

import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.core.task.TaskRejectedException

/**
 * Ignore errors from publishing events, which appear when
 * gracefully shutting down the server
 */
fun ApplicationEventPublisher.tryPublishEvent(event: Any) = try {
    when (event) {
        is ApplicationEvent -> publishEvent(event)
        else -> publishEvent(event)
    }
} catch (ex: TaskRejectedException) {
    logger.info("Failed to publish event ${event::class.qualifiedName}, ignoring")
}
