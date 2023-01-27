@file:Suppress("UnstableApiUsage")

import com.healthmetrix.deident.buildlogic.conventions.excludeReflect
import com.healthmetrix.deident.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.deident.buildlogic.conventions.exclusionsSpringTestRuntime
import com.healthmetrix.deident.buildlogic.conventions.registeringExtended
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.spring)
}

tasks.withType<BootBuildImage> {
    imageName.set("healthmetrixgmbh/deident")
}

dependencies {
    implementation(projects.commons)
    runtimeOnly(projects.downloader)
    runtimeOnly(projects.harmonizer)
    runtimeOnly(projects.deidentifier)
    runtimeOnly(projects.uploader)
    runtimeOnly(projects.persistence)
    runtimeOnly(projects.status)
    runtimeOnly(projects.contextualizer)
    runtimeOnly(projects.dateShifter)
    runtimeOnly(projects.statistics)
    runtimeOnly(projects.debugCache)
    runtimeOnly(projects.typeFilter)
    runtimeOnly(projects.patientDeduplicator)

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)

    // metrics
    implementation(libs.micrometer.cloudwatch2)

    testImplementation(projects.commonsTest)
    testImplementation(projects.persistence.jobApi)
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }

    // acceptance test
    testImplementation(libs.okhttp3.okhttp)
    testImplementation(libs.okhttp3.mockwebserver)

    implementation(libs.springdoc.openapi) { excludeReflect() }
    implementation(libs.springdoc.ui)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val acceptance by registeringExtended(test, libs.versions.junit.get()) {}
        val integration by registeringExtended(test, libs.versions.junit.get()) {}
    }
}

tasks.named("check") {
    dependsOn(testing.suites.named("acceptance"), testing.suites.named("integration"))
}
