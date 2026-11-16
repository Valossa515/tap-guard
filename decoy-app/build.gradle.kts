plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.valossa515.tapguard.decoy"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        applicationId = "io.github.valossa515.tapguard.decoy"
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
    }
}

kotlin {
    jvmToolchain(
        libs.versions.jdk
            .get()
            .toInt(),
    )
}

dependencies {
    testImplementation(libs.junit4)
}
