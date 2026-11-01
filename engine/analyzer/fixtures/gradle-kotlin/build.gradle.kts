plugins {
    id("java")
    alias(libs.plugins.kotlin.jvm)
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    api(libs.kotlin.stdlib)
    testImplementation(libs.junit.jupiter)
    api(project(":shared"))
}
