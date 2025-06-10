plugins {
    java
    kotlin("jvm") version "2.1.21-RC2"
    application
    id("me.champeau.jmh") version "0.7.2"
}

repositories {
    mavenCentral()
}

dependencies {
    jmh("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    jmh(project(":bitvector-core"))
    // https://mvnrepository.com/artifact/net.onedaybeard.artemis/artemis-odb
    jmh("net.onedaybeard.artemis:artemis-odb:2.3.0")
}

jmh {
    zip64 = true
    jmhVersion.set("1.37")
    failOnError = true // Should JMH fail immediately if any benchmark had experienced the unrecoverable error?
//    forceGC = false // Should JMH force GC between iterations?
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
}