import com.healthmetrix.deident.buildlogic.conventions.exclusionsTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(libs.kotlin.reflect)
    implementation(projects.commons)

    implementation(libs.spring.boot.boot)

    // as of hapi-fhir-structures-r4 needs this for FhirPath...
    runtimeOnly(libs.caffeine)

    // This is an optional dependency of the fhir structures that is not optional for FHIRPathEngine
    // https://github.com/jamesagnew/hapi-fhir/blob/0c31741eec86672a867b99373083993053732bff/hapi-fhir-structures-dstu3/pom.xml#L115
    runtimeOnly(libs.fhir.ucum) {
        exclude(group = "junit", module = "junit")
    }

    testImplementation(projects.commonsTest)
    testImplementation(libs.bundles.test.implementation)
    testRuntimeOnly(libs.bundles.test.runtime) { exclusionsTestRuntime() }
}
