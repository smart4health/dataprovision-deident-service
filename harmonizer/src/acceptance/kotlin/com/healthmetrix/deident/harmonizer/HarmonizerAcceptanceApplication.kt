package com.healthmetrix.deident.harmonizer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

// use a separate component scan to exclude things we don't want,
// like Harmonizer and its dependencies
@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(
    basePackageClasses = [HarmonizerAcceptanceConfiguration::class],
    excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = [Harmonizer::class])],
)
class HarmonizerAcceptanceApplication
