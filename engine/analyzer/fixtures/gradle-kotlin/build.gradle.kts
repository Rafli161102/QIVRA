plugins {
    id("java")
}
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
}
