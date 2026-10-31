import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 incluye el soporte de Kotlin: no se aplica org.jetbrains.kotlin.android.
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "gt.edu.umg.finanzas.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "gt.edu.umg.finanzas.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "0.4.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val filtroRed = if (providers.gradleProperty("pruebasEnVivo").isPresent) "annotation" else "notAnnotation"
        testInstrumentationRunnerArguments[filtroRed] = "gt.edu.umg.finanzas.app.PruebaEnVivo"
    }

    buildTypes {
        debug {
            // Instalación separada para que las pruebas de interfaz no vacíen la app de uso diario.
            if (providers.gradleProperty("datosPrueba").isPresent) applicationIdSuffix = ".pruebas"
        }
        release {
            isMinifyEnabled = false
        }
    }

    // Mismo bytecode 11 que el core (finanzas-core/pom.xml, propiedad bytecode.jvm).
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // java.time (LocalDate del core y de los mapeos) no existe antes de API 26:
        // el desugaring lo incluye en el APK para minSdk 24.
        isCoreLibraryDesugaringEnabled = true
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

// Room escribe aquí el esquema de cada versión; app/schemas se versiona en Git.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    constraints {
        // Room testing 2.8.5 necesita los métodos default de 1.8.1; con core 1.7.3 lanza AbstractMethodError.
        implementation(libs.kotlinx.serialization.core)
    }
    implementation(libs.finanzas.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
}
