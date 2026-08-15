plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.davemorrissey.labs.subscaleview.test"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.davemorrissey.labs.subscaleview.test"
        minSdk = 24
        targetSdk = 37

        versionCode = 4
        versionName = "3.1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    packaging {
        jniLibs.keepDebugSymbols.addAll(listOf("*/mips/*.so", "*/mips64/*.so"))
    }

    sourceSets {
        getByName("main").assets.srcDirs("assets")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":library"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.viewpager)
}
