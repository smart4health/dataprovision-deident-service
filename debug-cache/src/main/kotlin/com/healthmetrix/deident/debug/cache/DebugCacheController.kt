package com.healthmetrix.deident.debug.cache

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.DataFormatException
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.merge
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.deident.commons.ApiResponse
import com.healthmetrix.deident.commons.IngestionContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.asEntity
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotWritableException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import com.github.michaelbull.result.runCatching as catch

@Profile("debug-cache & !production & !prod")
@RestController
class DebugCacheController(
    private val inMemoryDebugCache: InMemoryDebugCache,
    private val fhirContext: FhirContext,
    private val objectMapper: ObjectMapper,
) {

    @GetMapping("/v1/debug/cache/job/last", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getLastJob(): ResponseEntity<CachedResourceResponse> = binding<CachedResourceResponse, CachedResourceResponse> {
        inMemoryDebugCache.getLast()
            .toResultOr { CachedResourceResponse.NotFound }
            .bind()
            .toResponse()
            .let(CachedResourceResponse::Found)
    }.merge().asEntity()

    @GetMapping("/v1/debug/cache/job/{jobId}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getJobById(
        @PathVariable
        jobId: String,
    ): ResponseEntity<CachedResourceResponse> = binding<CachedResourceResponse, CachedResourceResponse> {
        jobId.asJobId()
            .mapError { CachedResourceResponse.BadRequest("Failed to parse job id") }
            .bind()
            .let(inMemoryDebugCache::get)
            .toResultOr { CachedResourceResponse.NotFound }
            .bind()
            .toResponse()
            .let(CachedResourceResponse::Found)
    }.merge().asEntity()

    @GetMapping("/v1/debug/cache/jobs", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun searchJobsByUser(
        @RequestParam(name = "user")
        userId: String,
    ): ResponseEntity<CachedResourcesByUserResponse> =
        CachedResourcesByUserResponse(inMemoryDebugCache.findByUserId(userId).map { it.toResponse() }).asEntity()

    @PostMapping("/v1/debug/cache/clear")
    fun clear(): ResponseEntity<Unit> {
        inMemoryDebugCache.clear()
        return ResponseEntity.ok().build()
    }

    sealed class CachedResourceResponse : ApiResponse {

        data class Found(
            @JsonUnwrapped
            val resource: CachedResource,
        ) : CachedResourceResponse()

        data class BadRequest(
            val message: String,
        ) : CachedResourceResponse() {
            override val status = HttpStatus.BAD_REQUEST
        }

        object NotFound : CachedResourceResponse() {

            override val status = HttpStatus.NOT_FOUND
            override val hasBody = false
        }
    }

    data class CachedResourcesByUserResponse(
        val resources: List<CachedResource>,
    ) : ApiResponse

    data class CachedResource(
        val context: IngestionContext,
        val bundle: JsonNode,
    )

    private fun InMemoryDebugCache.CacheItem.toResponse() = try {
        CachedResource(
            context = this.context,
            bundle = objectMapper.readTree(fhirContext.newJsonParser().encodeResourceToString(fhirBundle)),
        )
    } catch (ex: DataFormatException) {
        throw HttpMessageNotWritableException(ex.message ?: "failed to write bundle to string")
    }

    private fun String.asJobId(): Result<JobId, Throwable> = catch {
        UUID.fromString(this)
    }
}
