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
        google()
        mavenCentral()
        // --- START NICE CXONE REPO INJECTION ---
        maven {
            name = "github-nice-devone-cxone-mobile"
            url = uri("https://maven.pkg.github.com/nice-devone/nice-cxone-mobile-sdk-android")
            credentials {
                // This checks for the property keys in local.properties (if loaded) or global
                // gradle.properties, falling back to environment variables.
                username = providers.gradleProperty("github.user").getOrNull() ?: System.getenv("GPR_USERNAME")
                password = providers.gradleProperty("github.key").getOrNull() ?: System.getenv("GPR_TOKEN")
            }
        }
        // --- END NICE CXONE REPO INJECTION ---
    }
}

rootProject.name = "Cxone Sample"
include(":app")
 