pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "tap-guard"

include(":core-domain")
include(":core-detection")
include(":core-nfc")
include(":core-data")
include(":app")
include(":decoy-app")
