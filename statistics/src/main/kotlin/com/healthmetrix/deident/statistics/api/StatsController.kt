package com.healthmetrix.deident.statistics.api

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.healthmetrix.deident.commons.ApiResponse
import com.healthmetrix.deident.commons.DocumentationConstants
import com.healthmetrix.deident.commons.asEntity
import com.healthmetrix.deident.commons.decodeUserSecret
import com.healthmetrix.deident.commons.hmacSha256
import com.healthmetrix.deident.commons.orThrow
import com.healthmetrix.deident.statistics.usecase.CountResourcesByUserUseCase
import com.healthmetrix.deident.statistics.usecase.CountTotalResourcesUseCase
import com.healthmetrix.deident.statistics.usecase.CountTotalUsersUseCase
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = DocumentationConstants.STATISTICS_API_TAG)
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
class StatsController(
    private val countResourcesByUserUseCase: CountResourcesByUserUseCase,
    private val countTotalUsersUseCase: CountTotalUsersUseCase,
    private val countTotalResourcesUseCase: CountTotalResourcesUseCase,
) {

    @Operation(summary = "Fetch user and global stats")
    @ApiResponses(
        value = [
            io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Returning Stats Item",
                content = [Content(schema = Schema(implementation = StatsApiResponse.Generated::class))],
            ),
        ],
    )
    @GetMapping("/v1/stats", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun generateStatistics(
        @Parameter(description = "Base64 encoded secret to deidentify the userId", example = "cGFzc3dvcmQ")
        @RequestHeader("X-Deident-User-Secret")
        userSecret: String,
        @Parameter(description = "Id unique per user", example = "123456")
        @RequestParam(name = "user")
        userId: String,
    ): ResponseEntity<StatsApiResponse> {
        val theUserId = userSecret
            .decodeUserSecret()
            .orThrow()
            .let(userId::hmacSha256)
        return StatsApiResponse.Generated(
            statistics = StatsResponse(
                user = StatsResponse.UserStats(resourcesUploadedCount = countResourcesByUserUseCase(userId = theUserId)),
                global = StatsResponse.GlobalStats(
                    usersCount = countTotalUsersUseCase(),
                    resourcesUploadedCount = countTotalResourcesUseCase(),
                ),
            ),
        ).asEntity()
    }

    sealed class StatsApiResponse : ApiResponse {
        data class Generated(
            @JsonUnwrapped
            val statistics: StatsResponse,
        ) : StatsApiResponse()
    }
}
