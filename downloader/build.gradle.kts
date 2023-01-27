import com.healthmetrix.deident.buildlogic.conventions.excludeReflect
import com.healthmetrix.deident.buildlogic.conventions.exclusionsSpringTestImplementation
import com.healthmetrix.deident.buildlogic.conventions.exclusionsSpringTestRuntime

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.kotlin.spring)
}

dependencies {
    implementation(libs.kotlin.reflect)

    implementation(projects.commons)
    implementation(projects.persistence.jobApi)

    implementation(libs.spring.framework.web)
    implementation(libs.spring.framework.context)
    implementation(libs.spring.framework.tx)

    implementation(libs.springdoc.openapi) { excludeReflect() }
    implementation(libs.springdoc.ui)

    testImplementation(projects.commonsTest)
    testImplementation(libs.bundles.test.implementation)
    testImplementation(libs.bundles.test.spring.implementation) { exclusionsSpringTestImplementation() }
    testRuntimeOnly(libs.bundles.test.spring.runtime) { exclusionsSpringTestRuntime() }
}
