package com.healthmetrix.deident

import com.healthmetrix.deident.commons.DocumentationConstants
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.tags.Tag
import org.hl7.fhir.r4.model.Bundle
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DocumentationConfig {
    @Bean
    fun api(documentationInfo: DocumentationInfo): OpenAPI {
        // This ignores HAPI types from being accessed by swagger which throws exceptions on a getter collision
        SpringDocUtils.getConfig().replaceWithSchema(Bundle::class.java, Schema<Void>())

        return OpenAPI()
            .info(documentationInfo.toApiInfo())
            .addTagsItem(statisticsApiTag)
            .addTagsItem(jobStatusApiTag)
            .addTagsItem(downloadControllerApiTag)
            .components(
                Components().addSecuritySchemes(
                    DocumentationConstants.USER_AUTHENTICATION_SCHEME,
                    basicAuthScheme,
                ),
            )
    }

    private val basicAuthScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("basic")
        .description("Basic auth credentials")
    private val jobStatusApiTag = Tag()
        .name(DocumentationConstants.JOB_STATUS_API_TAG)
        .description("Check a Job status")
    private val statisticsApiTag = Tag()
        .name(DocumentationConstants.STATISTICS_API_TAG)
        .description("Fetch user-specific and global statistics on provided resources")
    private val downloadControllerApiTag = Tag()
        .name(DocumentationConstants.DOWNLOADER_API_TAG)
        .description("Main entry point for the deident pipeline to upload FHIR bundles from the app to the RP")
}

@ConfigurationProperties(prefix = "documentation-info")
data class DocumentationInfo(
    val title: String,
    val description: String,
    val contact: ContactConfig,
) {
    data class ContactConfig(
        val name: String,
        val email: String,
    )

    fun toApiInfo(): Info {
        return Info()
            .title(title)
            .description(description)
            .contact(
                Contact()
                    .name(contact.name)
                    .email(contact.email),
            )
    }
}
