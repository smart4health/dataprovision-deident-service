@file:Suppress("UnstableApiUsage")

import com.healthmetrix.deident.buildlogic.conventions.excludeReflect
import com.healthmetrix.deident.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.deident.buildlogic.conventions.exclusionsSpringTestRuntime
import com.healthmetrix.deident.buildlogic.conventions.registeringExtended

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)

    implementation(libs.spring.framework.web)
    implementation(libs.spring.framework.context)
    implementation(libs.spring.framework.tx)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.ext.reactor)

    implementation(libs.springdoc.openapi) { excludeReflect() }
    implementation(libs.springdoc.ui)

    testImplementation(projects.commonsTest)
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val acceptance by registeringExtended(test, libs.versions.junit.get()) {}
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("acceptance"))
}
