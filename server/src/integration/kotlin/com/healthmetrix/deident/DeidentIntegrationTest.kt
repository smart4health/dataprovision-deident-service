package com.healthmetrix.deident

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.JobStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import java.time.ZonedDateTime
import java.util.Base64
import java.util.concurrent.TimeUnit

const val APPLICATION_FHIR_JSON_VALUE = "application/fhir+json"

/**
 * Tests the following steps so far:
 * - Downloader
 * - Contextualizer (setting the References straight)
 * - DateShifter
 * - Deidentifier (DateRedactMethod, KeepMethod, RedactMethod)
 * - Harmonizer: accesses dev instance of qomop-service
 * - Uploader
 */
@Suppress("FunctionName")
@SpringBootTest(
    classes = [DeidentApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["date-shift.fixed-date-shift-days=42", "spring.cloud.vault.enabled=false"],
)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class DeidentIntegrationTest {

    private var userId: String = "rmyehvnxb"
    private var userSecret: String = "hmiryxfss"
    private val d4lId: String = "constant-id"
    private lateinit var inputBundle: String
    private lateinit var expectedOutputBundle: String

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var researchPlatform: MockWebServer

    @BeforeEach
    fun before() {
        inputBundle = javaClass.classLoader.getResource("input.bundle.json")!!.readText()
        expectedOutputBundle = javaClass.classLoader.getResource("output.bundle.json")!!.readText()
        researchPlatform = MockWebServer().apply {
            start(4040)
            enqueue(MockResponse().setResponseCode(200))
        }
    }

    @AfterEach
    fun after() {
        researchPlatform.shutdown()
    }

    @Test
    fun `full loop from input json to output json`() {
        mockMvc
            .post("/v1/downloader/$userId") {
                headers {
                    add("Content-Type", APPLICATION_FHIR_JSON_VALUE)
                    add("X-Deident-User-Secret", String(Base64.getEncoder().encode(userSecret.toByteArray())))
                    add("X-Deident-D4l-Id", String(Base64.getEncoder().encode(d4lId.toByteArray())))
                    add("X-Deident-Fetched-At", ZonedDateTime.now().toString())
                }
                content = inputBundle
            }
            .andExpect { status { isOk() } }
            .andReturn()

        researchPlatform.takeRequest(15, TimeUnit.SECONDS)!!.let {
            assertThat(it.headers["Content-Type"]).startsWith("multipart/form-data;")

            // can't find a way to easily parse a multipart form body from the RecordedRequest,
            // so instead split on \r\n and pick the third-to-last element, because second-to-last
            // is the multipart boundary and the last element is empty, since http messages end with \r\n
            assertThat(it.body.readUtf8().split("\r\n").reversed().drop(2).first().json())
                .isEqualTo(expectedOutputBundle.json())
        }

        // wait for status updates to shake out
        Thread.sleep(1000)

        jobRepository.findJobsByUserId("B2EA9F9354FD7F412121533D7C27B67132AF2F3EDEA18066348237C560A5DFCB").let {
            assertThat(it).hasSize(1)
            assertThat(it[0].status).isEqualTo(JobStatus.Uploaded)
        }
    }

    private fun String.json() = objectMapper.readTree(this)
}
