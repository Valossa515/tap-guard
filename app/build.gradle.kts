plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "io.github.valossa515.tapguard"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        applicationId = "io.github.valossa515.tapguard"
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
    implementation(project(":core-domain"))
    implementation(project(":core-detection"))
    implementation(project(":core-nfc"))
    implementation(project(":core-data"))
    testImplementation(libs.junit4)
}

ktlint {
    version.set(libs.versions.ktlint)
}
