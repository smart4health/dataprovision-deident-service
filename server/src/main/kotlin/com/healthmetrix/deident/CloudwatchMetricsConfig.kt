package com.healthmetrix.deident

import io.micrometer.cloudwatch2.CloudWatchConfig
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry
import io.micrometer.core.instrument.Clock
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import java.time.Duration

@Configuration
@Profile("cloudwatch-metrics")
class CloudwatchMetricsConfig {

    @Bean
    fun provideConfig(
        configurationProperties: CloudWatchMetricsConfigurationProperties,
    ) = CloudWatchConfig { key ->
        when (key) {
            "cloudwatch.namespace" -> configurationProperties.namespace
            "cloudwatch.step" -> configurationProperties.step.toString()
            else -> null
        }
    }

    @Bean
    fun provideRegistry(config: CloudWatchConfig) = CloudWatchMeterRegistry(
        config,
        Clock.SYSTEM,
        CloudWatchAsyncClient.create(),
    )

    @ConfigurationProperties(prefix = "cloudwatch-metrics")
    @Profile("cloudwatch-metrics")
    data class CloudWatchMetricsConfigurationProperties(
        val namespace: String,
        val step: Duration,
    ) {
        init {
            if (namespace.isBlank()) {
                throw IllegalStateException("cloudwatch.namespace may not be blank")
            }
        }
    }
}
