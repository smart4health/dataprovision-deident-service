package com.healthmetrix.deident.debug.cache

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
@ComponentScan(
    basePackageClasses = [DebugCacheAcceptanceConfiguration::class],
)
class DebugCacheAcceptanceApplication
