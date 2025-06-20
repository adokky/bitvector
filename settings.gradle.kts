pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "bitvector"

include(":core")
include(":jvm-benchmarks")

// https://docs.gradle.org/8.11.1/userguide/configuration_cache.html#config_cache:stable
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")