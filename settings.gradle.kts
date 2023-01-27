@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }

    // Unstable as org.gradle.api.initialization.resolve.DependencyResolutionManagement is still @Incubating
    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "deident-service"
include("persistence")
include("persistence:job-api")
include("downloader")
include("deidentifier")
include("commons")
include("server")
include("harmonizer")
include("uploader")
include("commons-test")
include("status")
include("contextualizer")
include("date-shifter")
include("persistence:date-shift-api")
include("statistics")
include("debug-cache")
include("type-filter")
include("patient-deduplicator")
