rootProject.name = "gradle-kotlin"
include(":app", ":shared")
includeBuild("build-logic")
project(":app").projectDir = file("app")
