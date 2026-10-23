import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 incluye el soporte de Kotlin: no se aplica org.jetbrains.kotlin.android.
    alias(libs.plugins.android.application)
}

android {
    namespace = "gt.edu.umg.finanzas.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "gt.edu.umg.finanzas.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Mismo bytecode 11 que el core (finanzas-core/pom.xml, propiedad bytecode.jvm).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(libs.finanzas.core)
    implementation(libs.androidx.appcompat)
}
