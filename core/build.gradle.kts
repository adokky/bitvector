plugins {
    id("io.github.adokky.quick-mpp") version "0.10"
    signing
    `maven-publish`
    id("com.vanniktech.maven.publish") version "0.32.0"
}

version = "0.8.1"

dependencies {
    commonTestImplementation("io.github.adokky:equals-tester:0.1")
}

signing {
    useGpgCmd()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    coordinates(
        groupId = group.toString(),
        artifactId = rootProject.name,
        version = version.toString()
    )

    pom {
        name = "bitvector"
        description = "Uncompressed, dynamically resizeable bitset for Kotlin Multiplatform "
        inceptionYear = "2025"
        url = "https://github.com/adokky/bitvector"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "adokky"
                name = "Alexander Dokuchaev"
                url = "https://dokky.github.io"
            }
            developer {
                id = "junkdog"
                name = "Adrian Papari"
                url = "https://github.com/junkdog"
            }
        }
        scm {
            url = "https://github.com/adokky/bitvector"
            connection = "scm:git:git://github.com/adokky/bitvector.git"
            developerConnection = "scm:git:ssh://git@github.com/adokky/bitvector.git"
        }
    }
}

// Fix Gradle warning about signing tasks using publishing
// task outputs without explicit dependencies:
// https://github.com/gradle/gradle/issues/26091
tasks.withType<PublishToMavenRepository> {
    dependsOn(tasks.withType<Sign>())
}