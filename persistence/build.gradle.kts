@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.persistence.jobApi)
    implementation(projects.persistence.dateShiftApi)

    implementation(libs.spring.framework.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)

    runtimeOnly(libs.postgres)
    runtimeOnly(libs.bundles.liquibase)
}
