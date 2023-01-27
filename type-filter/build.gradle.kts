import com.healthmetrix.deident.buildlogic.conventions.exclusionsTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.persistence.jobApi)

    implementation(libs.spring.framework.context)
    implementation(libs.spring.framework.tx)

    testImplementation(projects.commonsTest)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime) { exclusionsTestRuntime() }
    testImplementation(libs.junit.jupiter.params)
}
