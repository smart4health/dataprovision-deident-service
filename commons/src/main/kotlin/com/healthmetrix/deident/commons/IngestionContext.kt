package com.healthmetrix.deident.commons

import java.time.ZonedDateTime
import java.util.UUID

typealias JobId = UUID

data class IngestionContext(
    val userId: String,
    val jobId: JobId,
    val fetchedAt: ZonedDateTime,
    val d4lId: String,
)
