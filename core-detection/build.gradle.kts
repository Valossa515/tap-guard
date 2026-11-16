plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.valossa515.tapguard.core.detection"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.minSdk
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
    testImplementation(libs.junit4)
}
