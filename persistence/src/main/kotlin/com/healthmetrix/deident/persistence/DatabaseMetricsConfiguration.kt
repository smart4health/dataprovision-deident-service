package com.healthmetrix.deident.persistence

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * Metrics name = db.table.size
 */
@Configuration
@Profile("rds | postgres")
class DatabaseMetricsConfiguration(
    private val registry: MeterRegistry,
    private val dataSource: DataSource,
) {

    @PostConstruct
    fun initializeTableSizeMetrics() {
        listOf("resource", "date_shift", "resource_with_job", "job").forEach { tableName ->
            DatabaseTableMetrics.monitor(registry, tableName, "deident", dataSource)
        }
    }
}
