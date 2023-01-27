package com.healthmetrix.deident.status.api

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.deident.commons.ApiResponse
import com.healthmetrix.deident.commons.DocumentationConstants
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.asEntity
import com.healthmetrix.deident.commons.decodeUserSecret
import com.healthmetrix.deident.commons.hmacSha256
import com.healthmetrix.deident.commons.orThrow
import com.healthmetrix.deident.persistence.job.api.Job
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.status.usecase.FindFirstRejectedJobUseCase
import com.healthmetrix.deident.status.usecase.SearchJobsByUserUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import com.github.michaelbull.result.runCatching as catch

@Tag(name = DocumentationConstants.JOB_STATUS_API_TAG)
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
class JobStatusController(
    private val jobRepository: JobRepository,
    private val searchJobsByUserUseCase: SearchJobsByUserUseCase,
    private val findFirstRejectedJobUseCase: FindFirstRejectedJobUseCase,
) {

    @Operation(summary = "Finds an existing Job and returns information about its status including temporal information")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Returning Job Status for the provided id",
                content = [Content(schema = Schema(implementation = JobStatusResponse.Found::class))],
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Bad Request",
                content = [Content(schema = Schema(implementation = JobStatusResponse.BadRequest::class))],
            ),
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Not Found",
                content = [Content(schema = Schema(implementation = JobStatusResponse.NotFound::class))],
            ),
        ],
    )
    @GetMapping("/v1/job/{jobId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getJob(
        @Parameter(description = "UUID identifying a job")
        @PathVariable
        jobId: String,
    ): ResponseEntity<JobStatusResponse> = binding<JobStatusResponse, JobStatusResponse> {
        jobId.asJobId()
            .mapError { JobStatusResponse.BadRequest("Failed to parse job id") }
            .bind()
            .let(jobRepository::findJobById)
            .toResultOr { JobStatusResponse.NotFound }
            .bind()
            .asResponse()
            .let(JobStatusResponse::Found)
    }.merge().asEntity()

    @Operation(summary = "Returns a list of processed jobs for a user")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Returns a list of the jobs, grouped by success, failure, rejected, or in progress",
                content = [Content(schema = Schema(implementation = JobSearchByUserResponse::class))],
            ),
        ],
    )
    @GetMapping("/v1/jobs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun searchJobsByUser(
        @Parameter(description = "Base64 encoded secret to deidentify the userId", example = "cGFzc3dvcmQ")
        @RequestHeader("X-Deident-User-Secret")
        userSecret: String,
        @Parameter(description = "Id unique per user", example = "123456")
        @RequestParam(name = "user")
        userId: String,
    ): ResponseEntity<JobSearchByUserResponse> {
        return userSecret
            .decodeUserSecret()
            .orThrow()
            .let(userId::hmacSha256)
            .let { searchJobsByUserUseCase(it) to findFirstRejectedJobUseCase(it) }
            .let { (res, reject) ->
                JobSearchByUserResponse(
                    lastSuccess = res.uploaded.firstOrNull()?.asResponse(),
                    lastFailure = res.errored.firstOrNull()?.asResponse(),
                    firstRejected = reject?.asResponse(),
                    inProgress = res.inProgress.map(Job::asResponse),
                )
            }
            .asEntity()
    }

    sealed class JobStatusResponse : ApiResponse {
        data class Found(
            @JsonUnwrapped
            val job: JobResponse,
        ) : JobStatusResponse()

        object NotFound : JobStatusResponse() {
            override val status = HttpStatus.NOT_FOUND

            override val hasBody = false
        }

        data class BadRequest(
            val message: String,
        ) : JobStatusResponse() {
            override val status = HttpStatus.BAD_REQUEST
        }
    }

    data class JobSearchByUserResponse(
        val lastSuccess: JobResponse?,
        val lastFailure: JobResponse?,
        val firstRejected: JobResponse?,
        val inProgress: List<JobResponse>,
    ) : ApiResponse
}

private fun String.asJobId(): Result<JobId, Throwable> = catch {
    UUID.fromString(this)
}
