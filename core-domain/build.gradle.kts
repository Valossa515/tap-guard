plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(
        libs.versions.jdk
            .get()
            .toInt(),
    )
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

ktlint {
    version.set(libs.versions.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("detekt.yml"))
    // A tarefa detekt (a do check) nao analisa src/testFixtures por padrao.
    source.from(files("src/testFixtures/kotlin"))
}

// RNF-08: minimo de 80% de cobertura de linhas no dominio. O check depende do koverVerify.
kover {
    // As fixtures de teste nao contam na cobertura do dominio.
    currentProject {
        sources {
            excludedSourceSets.add("testFixtures")
        }
    }
    reports {
        verify {
            rule {
                minBound(80)
            }
        }
    }
}
