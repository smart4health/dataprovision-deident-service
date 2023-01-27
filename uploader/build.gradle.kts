@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(projects.commons)

    implementation(libs.spring.framework.context)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.ext.reactor)

    implementation(libs.bouncycastle)
}
