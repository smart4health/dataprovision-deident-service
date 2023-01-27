package com.healthmetrix.deident.uploader

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import com.healthmetrix.deident.commons.logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

interface ResearchPlatformClient {
    fun upload(body: String, d4lId: String): Result<HttpStatusCode?, Throwable>
}

@Component
@Profile("!remote-rp & !local-rp")
class MockResearchPlatformClient : ResearchPlatformClient {
    override fun upload(body: String, d4lId: String): Result<HttpStatusCode?, Throwable> {
        logger.info("Mock upload of $body")
        return Ok(HttpStatus.OK)
    }
}

@Component
@Profile("remote-rp | local-rp")
class RemoteResearchPlatformClient(
    @Qualifier("rpWebClient")
    private val webClient: WebClient,
    @Value("\${rp.upload}")
    private val uploadEndpoint: String,
) : ResearchPlatformClient {
    override fun upload(body: String, d4lId: String): Result<HttpStatusCode?, Throwable> = runCatching {
        logger.info("Uploading to $uploadEndpoint")

        val multipartData = MultipartBodyBuilder().apply {
            part(d4lId, body)
        }.build()

        webClient.post()
            .uri(uploadEndpoint)
            .body(BodyInserters.fromMultipartData(multipartData))
            .exchangeToMono {
                Mono.just(it.statusCode())
            }
            .block()
    }
}
