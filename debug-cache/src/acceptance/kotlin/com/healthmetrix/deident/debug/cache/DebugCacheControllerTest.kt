package com.healthmetrix.deident.debug.cache

import ca.uhn.fhir.context.FhirContext
import com.healthmetrix.deident.commons.IngestionContext
import com.healthmetrix.deident.commons.JobId
import com.healthmetrix.deident.commons.Transaction
import com.healthmetrix.deident.commons.test.FhirHelper
import org.hamcrest.Matchers
import org.hl7.fhir.r4.model.Basic
import org.hl7.fhir.r4.model.Bundle
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("FunctionName")
@AutoConfigureMockMvc
@SpringBootTest(properties = ["debug-cache-max-size=2", "spring.cloud.vault.enabled=false"])
@ActiveProfiles("debug-cache")
class DebugCacheControllerTest {

    private lateinit var basicResource: Basic

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var inMemoryDebugCache: InMemoryDebugCache

    @Autowired
    private lateinit var fhirContext: FhirContext

    private val fhirHelper by lazy { FhirHelper(fhirContext) }

    @BeforeEach
    fun beforeEach() {
        basicResource = fhirHelper.load("harmonization_result.json")
    }

    @Test
    fun `test the debug cache also exceeding the max limit`() {
        val transaction = Transaction(toCreate = listOf(basicResource)).asBundle()
        val firstJob = JobId.randomUUID()
        val secondJob = JobId.randomUUID()

        inMemoryDebugCache.clear()
        inMemoryDebugCache.put(firstJob.toFakeCacheItem(transaction, "1"))
        inMemoryDebugCache.put(secondJob.toFakeCacheItem(transaction, "2"))

        // Test existence of 1st Job
        mockMvc
            .get("/v1/debug/cache/job/$firstJob")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.context.jobId", Matchers.`is`(firstJob.toString())) }
            .andExpect { jsonPath("$.bundle.entry[0].resource.resourceType", Matchers.`is`("Basic")) }
            .andReturn()

        // Test existence of 2nd Job
        mockMvc
            .get("/v1/debug/cache/job/$secondJob")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.context.jobId", Matchers.`is`(secondJob.toString())) }
            .andExpect { jsonPath("$.bundle.entry[0].resource.resourceType", Matchers.`is`("Basic")) }
            .andReturn()

        // Test correct order for last job
        mockMvc
            .get("/v1/debug/cache/job/last")
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.context.jobId", Matchers.`is`(secondJob.toString())) }
            .andExpect { jsonPath("$.bundle.entry[0].resource.resourceType", Matchers.`is`("Basic")) }
            .andReturn()

        // Add 3rd job pushing the 1st one out due to max limit of 2
        inMemoryDebugCache.put(JobId.randomUUID().toFakeCacheItem(transaction, "2"))
        mockMvc
            .get("/v1/debug/cache/job/$firstJob")
            .andExpect { status { isNotFound() } }
            .andReturn()

        mockMvc
            .get("/v1/debug/cache/jobs") {
                param("user", "2")
            }
            .andExpect { status { isOk() } }
            .andExpect { jsonPath("$.resources.length()", Matchers.`is`(2)) }
            .andReturn()

        // clear all
        mockMvc
            .post("/v1/debug/cache/clear")
            .andExpect { status { isOk() } }
            .andReturn()
        mockMvc
            .get("/v1/debug/cache/job/last")
            .andExpect { status { isNotFound() } }
            .andReturn()
    }
}

private fun UUID.toFakeCacheItem(bundle: Bundle, userId: String) = InMemoryDebugCache.CacheItem(
    context = IngestionContext(
        userId,
        this,
        ZonedDateTime.now(),
        "p-123",
    ),
    fhirBundle = bundle,
)
