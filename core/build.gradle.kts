plugins {
    id("io.github.adokky.quick-mpp") version "0.22"
    id("io.github.adokky.quick-publish") version "0.22"
}

version = "0.9.3"

dependencies {
    commonTestImplementation("io.github.adokky:equals-tester:1.1.0")
}

mavenPublishing {
    coordinates(artifactId = "bitvector")
    pom {
        name = "bitvector"
        description = "Uncompressed, dynamically resizeable bitset for Kotlin Multiplatform "
        inceptionYear = "2025"
        developers {
            developer {
                id = "junkdog"
                name = "Adrian Papari"
                url = "https://github.com/junkdog"
            }
        }
    }
}