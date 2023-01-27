import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.gradle.versions)

    // https://youtrack.jetbrains.com/issue/KT-30276
    alias(libs.plugins.kotlin.jvm) apply false

    // Run local tasks
    id("com.healthmetrix.kotlin.localdriver")
}

allprojects {
    group = "com.healthmetrix"
    version = "1.0-SNAPSHOT"
}

tasks.withType<DependencyUpdatesTask> {
    outputFormatter = closureOf<Result> {
        val sb = StringBuilder()
        outdated.dependencies.forEach { dep ->
            sb.append("${dep.group}:${dep.name} ${dep.version} -> ${dep.available.release ?: dep.available.milestone}\n")
        }
        if (sb.isNotBlank()) {
            rootProject.file("build/dependencyUpdates/outdated-dependencies").apply {
                parentFile.mkdirs()
                println(sb.toString())
                writeText(sb.toString())
            }
        } else {
            println("Up to date!")
        }
    }

    // no alphas, betas, milestones, release candidates
    // or whatever the heck jaxb-api is using
    rejectVersionIf {
        candidate.isBadHapi or
            candidate.version.contains("alpha", ignoreCase = true) or
            candidate.version.contains("beta", ignoreCase = true) or
            candidate.version.contains(Regex("M[0-9]*$")) or
            candidate.version.contains("RC", ignoreCase = true) or
            candidate.version.contains(Regex("b[0-9]+\\.[0-9]+$")) or
            candidate.version.contains("eap")
    }
}

// 14. July 2021: 5.4+ uses caffeine 3.0+ which is incompatible with Android < 26. This app isn't android, but we should use the same here
val ModuleComponentIdentifier.isBadHapi: Boolean
    get() = (group == "ca.uhn.hapi.fhir") and (version.replace(".", "").toInt() >= 540)

tasks.register<com.healthmetrix.deident.buildlogic.localdriver.SubmitBundleTask>("submitBundle")
tasks.register<com.healthmetrix.deident.buildlogic.localdriver.FhirXmlToJsonTask>("xmlToJson")
tasks.register<com.healthmetrix.deident.buildlogic.localdriver.FhirMumcCsvToJsonTask>("mumcToJson")
