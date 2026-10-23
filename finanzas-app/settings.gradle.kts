pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // El core propio sale SOLO de ~/.m2 (lo publica `mvn clean install`), y ~/.m2 solo
        // se consulta para ese grupo. No existe otra ruta para el JAR (sin carpeta repositorio/).
        exclusiveContent {
            forRepository { mavenLocal() }
            filter { includeGroup("gt.edu.umg.finanzas") }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "finanzas-app"
include(":app")
