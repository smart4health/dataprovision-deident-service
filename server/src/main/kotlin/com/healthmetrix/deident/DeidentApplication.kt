package com.healthmetrix.deident

import org.springframework.boot.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

/**
 * Exclusions:
 * - DataSourceAutoConfiguration: to allow in memory db on local, see com.healthmetrix.deident.persistence.DatabaseConfiguration
 * - RepositoryMetricsAutoConfiguration: we don't need crud repositories being auto timed for now
 */
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class, RepositoryMetricsAutoConfiguration::class])
@ConfigurationPropertiesScan
class DeidentApplication

fun main(args: Array<String>) {
    runApplication<DeidentApplication>(*args)
}
