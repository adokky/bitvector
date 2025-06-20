plugins {
    id("io.github.adokky.quick-mpp") version "0.14"
    id("io.github.adokky.quick-publish") version "0.14"
}

version = "0.9.1"

dependencies {
    commonTestImplementation("io.github.adokky:equals-tester:0.1")
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