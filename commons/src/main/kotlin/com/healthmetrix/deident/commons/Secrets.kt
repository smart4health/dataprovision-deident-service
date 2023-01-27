package com.healthmetrix.deident.commons

import com.amazonaws.secretsmanager.caching.SecretCache
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.github.michaelbull.result.toResultOr
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

enum class SecretKey(
    val location: String,
    val isOmop: Boolean = false,
) {
    DB_CREDENTIALS("rds-credentials"),
    RP_SERVER_CERT("research-platform/server-cert"), // X509Certificate
    RP_CLIENT_CERT("research-platform/client-cert"), // X509Certificate
    RP_CLIENT_KEY("research-platform/client-key"), // PKCS8 key, in a pem
    QOMOP_CREDENTIALS("deident-credentials", isOmop = true),
}

interface Secrets {
    operator fun get(key: SecretKey): String // missing secrets are fatal
}

@Suppress("unused")
@Component
@Profile("!secrets-aws & !secrets-vault")
internal class MockSecrets(private val objectMapper: ObjectMapper) : Secrets {
    override operator fun get(key: SecretKey): String = when (key) {
        SecretKey.DB_CREDENTIALS -> object {
            val username = "username"
            val password = "password"
        }.let(objectMapper::writeValueAsString)
        SecretKey.RP_SERVER_CERT -> "NOT A CERT"
        SecretKey.RP_CLIENT_CERT -> "ALSO NOT A CERT"
        SecretKey.RP_CLIENT_KEY -> "NOT A KEY"
        SecretKey.QOMOP_CREDENTIALS -> object {
            val username = "qomop-username"
            val password = "qomop-passwod"
        }.let(objectMapper::writeValueAsString)
    }
}

@Component
@Profile("secrets-aws")
internal class AwsSecrets(
    private val secretCache: SecretCache,
    @Value("\${secrets.namespace}")
    namespace: String,
    @Value("\${secrets.qomop-namespace}")
    qomopNamespace: String,
) : Secrets {
    private val namespace: String
    private val qomopNamespace: String

    init {
        if (namespace.endsWith("/")) {
            logger.warn("secrets.namespace should not end with slash, trimming")
        }

        this.namespace = namespace.trimEnd('/')

        if (qomopNamespace.endsWith("/")) {
            logger.warn("secrets.qomop-namespace should not end with slash, trimming")
        }

        this.qomopNamespace = qomopNamespace.trimEnd('/')
    }

    override operator fun get(key: SecretKey): String {
        if (key.location.startsWith("/")) {
            logger.warn("$key starts with slash, trimming")
        }

        val prefix = if (key.isOmop) qomopNamespace else namespace

        val secretId = "$prefix/${key.location.trimEnd('/')}"

        return secretCache.runCatching {
            getSecretString(secretId)
        }.flatMap {
            it.toResultOr { NullPointerException() }
        }.mapError {
            RuntimeException("Failed to retrieve secret $secretId", it)
        }.orThrow()
    }
}

@Configuration
open class SecretsConfiguration {
    @Bean
    @Profile("secrets-aws")
    open fun provideSecretCache(): SecretCache = SecretCache()
}

@Configuration
@Profile("secrets-vault")
open class VaultConfiguration {

    @ConfigurationProperties("rds-credentials")
    @Profile("secrets-vault")
    data class RdsCredentials(
        val username: String,
        val password: String,
    )

    @ConfigurationProperties("research-platform")
    @Profile("secrets-vault")
    data class ResearchPlatform(
        val clientCert: String,
        val serverCert: String,
        val clientKey: String,
    )
}

@Suppress("unused")
@Component
@Profile("secrets-vault")
internal class VaultSecrets(
    private val rdsCredentials: VaultConfiguration.RdsCredentials,
    private val researchPlatform: VaultConfiguration.ResearchPlatform,
    private val objectMapper: ObjectMapper,
) : Secrets {
    override operator fun get(key: SecretKey): String = when (key) {
        SecretKey.DB_CREDENTIALS -> object {
            val username = rdsCredentials.username
            val password = rdsCredentials.password
        }.let(objectMapper::writeValueAsString)
        SecretKey.RP_SERVER_CERT -> researchPlatform.serverCert
        SecretKey.RP_CLIENT_CERT -> researchPlatform.clientCert
        SecretKey.RP_CLIENT_KEY -> researchPlatform.clientKey
        SecretKey.QOMOP_CREDENTIALS -> throw Exception("Not configured as currently not required for prod")
    }
}
