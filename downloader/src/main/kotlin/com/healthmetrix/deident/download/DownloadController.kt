package com.healthmetrix.deident.download

import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.healthmetrix.deident.commons.ApiResponse
import com.healthmetrix.deident.commons.DocumentationConstants
import com.healthmetrix.deident.commons.DownloadedEvent
import com.healthmetrix.deident.commons.IngestionContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.asEntity
import com.healthmetrix.deident.commons.decodeUserSecret
import com.healthmetrix.deident.commons.hmacSha256
import com.healthmetrix.deident.commons.orThrow
import com.healthmetrix.deident.commons.tryPublishEvent
import com.healthmetrix.deident.download.fhir.APPLICATION_FHIR_JSON_VALUE
import io.micrometer.core.annotation.Timed
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.hl7.fhir.r4.model.Bundle
import org.springframework.context.ApplicationEventPublisher
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.ZonedDateTime
import io.swagger.v3.oas.annotations.parameters.RequestBody as DocRequestBody

@Tag(name = DocumentationConstants.DOWNLOADER_API_TAG)
@SecurityRequirement(name = DocumentationConstants.USER_AUTHENTICATION_SCHEME)
@ApiResponses(
    value = [
        io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "Unauthorized action",
            content = [Content()],
        ),
    ],
)
@RestController
@RequestMapping("/v1/downloader")
class DownloadController(
    private val createJobUseCase: CreateJobUseCase,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val deidentifyIdentifiersUseCase: DeidentifyIdentifiersUseCase,
) {

    @Operation(summary = "Provide a FHIR bundle in order to be processed and uploaded to the research platform")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Resource was added to the pipeline successfully. Check Job API for Status",
                content = [Content(schema = Schema(implementation = DownloadResponse.Success::class))],
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "The D4L Id could not be decoded from the headers",
                content = [Content(schema = Schema(implementation = DownloadResponse.BadD4lId::class))],
            ),
        ],
    )
    @PostMapping(
        value = ["/{userId}"],
        produces = [MediaType.APPLICATION_JSON_VALUE],
        consumes = [APPLICATION_FHIR_JSON_VALUE],
    )
    @Timed(value = "s4h.downloader.timed", description = "Time taken for Downloader")
    fun uploadBatch(
        @Parameter(
            description = "Time of the bundle being fetched as ZonedDateTime",
            example = "2021-04-10T01:30:00.000-05:00",
        )
        @RequestHeader("X-Deident-Fetched-At")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        fetchedAt: ZonedDateTime,
        @Parameter(description = "Base64 encoded secret to deidentify the userId and bundle", example = "cGFzc3dvcmQ")
        @RequestHeader("X-Deident-User-Secret")
        userSecret: String,
        @Parameter(description = "Base64 encoded Data4Life identifier to add context to the resources missing them")
        @RequestHeader("X-Deident-D4l-Id")
        d4lId: String,
        @Parameter(description = "Id unique per user", example = "123456")
        @PathVariable
        userId: String,
        @DocRequestBody(description = "A valid FHIR R4 Bundle. See official HL7 documentation for details.")
        @RequestBody
        bundle: Bundle,
    ): ResponseEntity<DownloadResponse> = binding<DownloadResponse.Success, DownloadResponse> {
        // TODO validate bundle, possibly with D4L lib, or with HAPI

        val secret = userSecret.decodeUserSecret().orThrow()

        @Suppress("NAME_SHADOWING")
        val d4lId = d4lId.decodeD4lId()
            .mapError { DownloadResponse.BadD4lId }
            .bind()
            .hmacSha256(secret)

        // TODO could be replaced by splitting deidented/non deidented values with types
        @Suppress("NAME_SHADOWING")
        val userId = userId.hmacSha256(secret)

        deidentifyIdentifiersUseCase(bundle, secret).orThrow()

        val (job, transaction) = createJobUseCase(userId, bundle)

        DownloadedEvent(IngestionContext(userId, job.id, fetchedAt, d4lId), transaction)
            .let(applicationEventPublisher::tryPublishEvent)

        DownloadResponse.Success(job.id)
    }.merge().asEntity()

    sealed class DownloadResponse : ApiResponse {

        data class Success(
            val jobId: JobId,
        ) : DownloadResponse()

        object BadD4lId : DownloadResponse() {
            override val status = HttpStatus.BAD_REQUEST

            @Suppress("MayBeConstant", "unused")
            val message = "D4L id failed to decode"
        }
    }
}
