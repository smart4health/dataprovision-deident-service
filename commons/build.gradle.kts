@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
}

dependencies {
    implementation(libs.kotlin.reflect)

    api(libs.slf4j.api)
    api(libs.logback.encoder)

    implementation(libs.spring.framework.web)
    implementation(libs.spring.framework.context)
    api(libs.jackson.kotlin) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
    }

    implementation(libs.aws.secretsmanager)
    implementation(libs.spring.cloud.vault.config)

    api(libs.micrometer.prometheus)

    // fhir
    api(libs.hapi.base)
    api(libs.hapi.r4)

    // kotlin-result
    api(libs.result)
}
