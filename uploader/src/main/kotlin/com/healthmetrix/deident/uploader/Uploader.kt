package com.healthmetrix.deident.uploader

import ca.uhn.fhir.context.FhirContext
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.map
import com.github.michaelbull.result.merge
import com.healthmetrix.deident.commons.ErrorEvent
import com.healthmetrix.deident.commons.HarmonizedEvent
import com.healthmetrix.deident.commons.UploadedEvent
import com.healthmetrix.deident.commons.kv
import com.healthmetrix.deident.commons.logger
import com.healthmetrix.deident.commons.tryPublishEvent
import io.micrometer.core.annotation.Timed
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class Uploader(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val fhirContext: FhirContext,
    private val researchPlatformClient: ResearchPlatformClient,
) {

    @EventListener
    @Timed(value = "s4h.uploader.timed", description = "Time taken for Uploader")
    fun onHarmonizedEvent(event: HarmonizedEvent) {
        logger.info(
            "Uploading resources {}",
            "count" kv event.transaction.all.size,
            "jobId" kv event.context.jobId,
        )

        if (event.transaction.all.isEmpty()) {
            UploadedEvent(event.context)
                .let(applicationEventPublisher::tryPublishEvent)

            return
        }

        val transactionBundle = event.transaction.asBundle()
        val body = fhirContext.newJsonParser().encodeResourceToString(transactionBundle)
        val statusCode = researchPlatformClient.upload(body, event.context.d4lId)

        when (statusCode) {
            Ok(HttpStatus.OK) -> UploadedEvent(event.context)
            else ->
                statusCode
                    .map { UploadException("Received status $statusCode instead of 200") }
                    .merge()
                    .let { ErrorEvent(event.context, it) }
        }.let(applicationEventPublisher::tryPublishEvent)
    }

    class UploadException(msg: String) : Exception(msg)
}
