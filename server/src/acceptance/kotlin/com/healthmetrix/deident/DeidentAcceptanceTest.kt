package com.healthmetrix.deident

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.deident.persistence.job.api.JobRepository
import com.healthmetrix.deident.persistence.job.api.JobStatus
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
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
 * - Harmonizer: by default all harmonized because of missing qomop profile! See DeidentIntegrationTest for full test
 * - Uploader
 * - Stats endpoint
 */
@Suppress("FunctionName")
@SpringBootTest(
    classes = [DeidentApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["date-shift.fixed-date-shift-days=42", "spring.cloud.vault.enabled=false"],
)
@AutoConfigureMockMvc
@ActiveProfiles("acceptance")
class DeidentAcceptanceTest {

    private var userId: String = "rmyehvnxb"
    private var userSecret: String = "hmiryxfsc"
    private val d4lId: String = "constant-id"
    private lateinit var inputBundle: String
    private lateinit var expectedOutputBundle: String
    private lateinit var secondInputBundle: String

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jobRepository: JobRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var researchPlatform: MockWebServer

    @BeforeEach
    fun before() {
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
    @Order(1)
    fun `full loop from input json to output json`() {
        inputBundle = javaClass.classLoader.getResource("input.bundle.json")!!.readText()
        expectedOutputBundle = javaClass.classLoader.getResource("output.bundle.json")!!.readText()
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

        researchPlatform.takeRequest(10, TimeUnit.SECONDS)!!.let {
            assertThat(it.headers["Content-Type"]).startsWith("multipart/form-data;")

            // can't find a way to easily parse a multipart form body from the RecordedRequest,
            // so instead split on \r\n and pick the third-to-last element, because second-to-last
            // is the multipart boundary and the last element is empty, since http messages end with \r\n
            assertThat(it.body.readUtf8().split("\r\n").reversed().drop(2).first().json())
                .isEqualTo(expectedOutputBundle.json())
        }

        // wait for status updates to shake out
        Thread.sleep(1000)

        jobRepository.findJobsByUserId("1CD5A835A8FAC94CA71E1FDEE5761B775C3E885E64F0648D2C30CCAB42E7409A").let {
            assertThat(it).hasSize(1)
            assertThat(it[0].status).isEqualTo(JobStatus.Uploaded)
        }

        mockMvc
            .get("/v1/stats?user=$userId") {
                headers {
                    add("X-Deident-User-Secret", String(Base64.getEncoder().encode(userSecret.toByteArray())))
                }
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.user.resourcesUploadedCount", `is`(14)) }
            .andExpect { jsonPath("$.global.usersCount", `is`(1)) }
            .andExpect { jsonPath("$.global.resourcesUploadedCount", `is`(14)) }
            .andReturn()
    }

    @Test
    @Order(2)
    fun `upload another resource to test the stats endpoint`() {
        secondInputBundle = javaClass.classLoader.getResource("input2.bundle.json")!!.readText()

        // Upload second bundle with 1 new resource
        mockMvc
            .post("/v1/downloader/$userId") {
                headers {
                    add("Content-Type", APPLICATION_FHIR_JSON_VALUE)
                    add("X-Deident-User-Secret", String(Base64.getEncoder().encode(userSecret.toByteArray())))
                    add("X-Deident-D4l-Id", String(Base64.getEncoder().encode(d4lId.toByteArray())))
                    add("X-Deident-Fetched-At", ZonedDateTime.now().toString())
                }
                content = secondInputBundle
            }
            .andExpect { status { isOk() } }
            .andReturn()

        // wait for status updates to shake out
        Thread.sleep(1000)

        jobRepository.findJobsByUserId("1CD5A835A8FAC94CA71E1FDEE5761B775C3E885E64F0648D2C30CCAB42E7409A").let {
            assertThat(it).hasSize(2)
            assertThat(it[1].status).isEqualTo(JobStatus.Uploaded)
        }

        // Check if stats is incremented by one
        mockMvc
            .get("/v1/stats?user=$userId") {
                headers {
                    add("X-Deident-User-Secret", String(Base64.getEncoder().encode(userSecret.toByteArray())))
                }
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.user.resourcesUploadedCount", `is`(15)) }
            .andExpect { jsonPath("$.global.usersCount", `is`(1)) }
            .andExpect { jsonPath("$.global.resourcesUploadedCount", `is`(15)) }
            .andReturn()
    }

    private fun String.json() = objectMapper.readTree(this)
}
