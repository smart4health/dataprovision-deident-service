package com.healthmetrix.deident.harmonizer.qomopclient

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.deident.commons.SecretKey
import com.healthmetrix.deident.commons.Secrets
import com.healthmetrix.deident.commons.logger
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
@Profile("qomop")
class QomopClientConfiguration {

    @Bean("qomopWebClient")
    fun provideQomopWebClient(
        @Value("\${qomop.base-url}")
        baseUrl: String,
        qomopBasicAuthCredentials: QomopBasicAuthCredentials?,
    ): WebClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofMillis(5000))
            .doOnConnected { conn: Connection ->
                conn.addHandlerLast(ReadTimeoutHandler(5000, TimeUnit.MILLISECONDS))
                    .addHandlerLast(WriteTimeoutHandler(5000, TimeUnit.MILLISECONDS))
            }

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(baseUrl)
            .defaultHeaders { headers ->
                qomopBasicAuthCredentials?.let { headers.setBasicAuth(it.username, it.password) }
                headers.contentType = MediaType.APPLICATION_JSON
            }
            .filter(logRequest())
            .filter(logResponse())
            .build()
    }

    private fun logRequest(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofRequestProcessor { clientRequest: ClientRequest ->
            logger.info("Request {}: {} {}", clientRequest.logPrefix(), clientRequest.method(), clientRequest.url())
            Mono.just(clientRequest)
        }

    private fun logResponse(): ExchangeFilterFunction =
        ExchangeFilterFunction.ofResponseProcessor { clientResponse: ClientResponse ->
            logger.info("Response {}: {}", clientResponse.logPrefix(), clientResponse.statusCode())
            Mono.just(clientResponse)
        }

    @Bean
    @Profile("!prod")
    fun provideBasicAuthCredentials(objectMapper: ObjectMapper, secrets: Secrets): QomopBasicAuthCredentials? =
        secrets[SecretKey.QOMOP_CREDENTIALS].let { objectMapper.readValue(it, QomopBasicAuthCredentials::class.java) }

    data class QomopBasicAuthCredentials(
        val username: String,
        val password: String,
    )
}
