plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(mainLibs.kotlin.gradle.plugin)

    implementation(mainLibs.http4k.core) { excludeKotlin() }
    implementation(mainLibs.http4k.server.netty) { excludeKotlin() }
    implementation(mainLibs.http4k.client.apache) { excludeKotlin() }

    implementation(mainLibs.kotlinx.html) { excludeKotlin() }

    // fhir
    implementation(mainLibs.hapi.base)
    implementation(mainLibs.hapi.r4)
    implementation(mainLibs.jackson.kotlin) { excludeReflect() }

    implementation(mainLibs.opencsv)
}

fun ExternalModuleDependency.excludeKotlin() {
    exclude(group = "org.jetbrains.kotlin")
}

fun ExternalModuleDependency.excludeReflect() {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
}
