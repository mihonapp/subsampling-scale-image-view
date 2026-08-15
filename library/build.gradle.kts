plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.davemorrissey.labs.subscaleview"
    compileSdk = 37

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("proguard-rules.txt")

        externalNativeBuild {
            cmake {
                cppFlags("-O3 -flto")
                targets("ep_ssiv_crop")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.image.decoder)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.tachiyomiorg"
                artifactId = "subsampling-scale-image-view"
                version = "4.0.0"

                from(components["release"])
            }
        }

        repositories {
            maven {
                name = "jitpack"
                url = uri("https://jitpack.io")
            }
        }
    }
}
