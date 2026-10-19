pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // JitPack redirects mavenCentral() to a Google mirror that lags behind
        if (System.getenv("JITPACK") == "true") {
            maven(url = "https://repo1.maven.org/maven2/")
        } else {
            mavenCentral()
        }
        google()
    }
}

include(":library", ":sample")
