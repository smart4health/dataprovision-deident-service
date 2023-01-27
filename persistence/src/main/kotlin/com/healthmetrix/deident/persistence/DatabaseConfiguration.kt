package com.healthmetrix.deident.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.healthmetrix.deident.commons.SecretKey
import com.healthmetrix.deident.commons.Secrets
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories
class DatabaseConfiguration {

    // auto configuration disabled to allow default profile with no jpa
    @Bean
    @Primary
    @Profile("rds | postgres")
    @ConfigurationProperties("spring.datasource")
    fun provideDataSourceProperties() = DataSourceProperties()

    @Bean
    @Profile("rds")
    fun provideCredentials(secrets: Secrets, objectMapper: ObjectMapper): Credentials =
        secrets[SecretKey.DB_CREDENTIALS].let(objectMapper::readValue)

    @Bean
    @Primary
    @Profile("postgres")
    fun providePostgresDatasource(
        dataSourceProperties: DataSourceProperties,
    ): DataSource = dataSourceProperties.initializeDataSourceBuilder().build()

    @Bean
    @Primary
    @Profile("rds")
    fun provideRdsDataSource(
        dataSourceProperties: DataSourceProperties,
        rds: Rds,
        credentials: Credentials,
    ): DataSource = with(dataSourceProperties) {
        url = "jdbc:postgresql://${rds.endpoint}/${rds.databaseName}"
        username = credentials.username
        password = credentials.password

        initializeDataSourceBuilder()
    }.build()

    data class Credentials(
        val username: String,
        val password: String,
    )

    @ConfigurationProperties("rds")
    @Profile("rds")
    data class Rds(
        val endpoint: String,
        val databaseName: String,
    )
}
