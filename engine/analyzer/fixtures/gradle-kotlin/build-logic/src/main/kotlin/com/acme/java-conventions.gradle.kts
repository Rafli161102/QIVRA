plugins {
    id("java")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}
