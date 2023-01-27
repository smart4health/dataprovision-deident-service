import com.healthmetrix.deident.buildlogic.conventions.exclusionsTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)

    implementation(libs.spring.framework.context)

    testImplementation(projects.commonsTest)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime) { exclusionsTestRuntime() }
}
