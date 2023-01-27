package com.healthmetrix.deident.harmonizer.qomopclient

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import io.micrometer.core.annotation.Counted
import org.hl7.fhir.r4.model.Coding
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

const val HARMONIZER_ENDPOINT = "/v1/harmonizer/coding"
const val METRICS_NAME_QOMOP_REQUESTS = "s4h.harmonizer.qomop.requests"

interface QomopClient {
    fun harmonize(coding: Coding): Result<HarmonizationResponse, Throwable>
}

@Component
@Profile("!qomop")
class MockQomopClient : QomopClient {
    @Counted(value = METRICS_NAME_QOMOP_REQUESTS, description = "Amount of harmonization requests to Qomop Service")
    override fun harmonize(coding: Coding): Result<HarmonizationResponse, Throwable> = Ok(
        HarmonizationResponse(
            success = true,
            message = "SuccessAlreadyHarmonized, harmonized=true, standardized=true",
            harmonizedCoding = coding.toQomopCoding(),
            harmonized = true,
            standardized = true,
        ),
    )
}

@Component
@Profile("qomop")
class RemoteQomopClient(
    @Qualifier("qomopWebClient")
    private val webClient: WebClient,
) : QomopClient {
    @Counted(value = METRICS_NAME_QOMOP_REQUESTS, description = "Amount of harmonization requests to Qomop Service")
    override fun harmonize(coding: Coding): Result<HarmonizationResponse, Throwable> = runCatching {
        webClient.post()
            .uri(HARMONIZER_ENDPOINT)
            .bodyValue(coding.toQomopCoding())
            .retrieve()
            .bodyToMono(HarmonizationResponse::class.java)
            .block()!!
    }
}

private fun Coding.toQomopCoding(): QomopCoding = QomopCoding(system = system, code = code, display = display)

data class QomopCoding(
    val system: String?,
    val code: String?,
    val display: String? = null,
)

data class HarmonizationResponse(
    val success: Boolean,
    val message: String,
    val harmonizedCoding: QomopCoding?,
    val harmonized: Boolean?,
    val standardized: Boolean?,
)
