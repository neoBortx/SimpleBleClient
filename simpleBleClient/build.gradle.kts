import org.jetbrains.dokka.DokkaConfiguration.Visibility
import org.jetbrains.dokka.gradle.DokkaTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.klint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    `maven-publish`
}

val versionLib = project.property("VERSION_LIB")

android {

    namespace = "com.bortxapps.simplebleclient"
    compileSdk = 34
    version = "$versionLib"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            version = "$versionLib-debug"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            version = "$versionLib"
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xexplicit-api=strict")
    }

    // Configure AAR filename with version
    libraryVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                if (output.name.contains("release")) {
                    output.outputFileName = "simplebleclient_$versionLib.aar"
                } else {
                    output.outputFileName = "simplebleclient_debug_$versionLib.aar"
                }
            }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

detekt {
    config.setFrom("detekt.yml")
}

dependencies {
    implementation(libs.core.ktx)
    testImplementation(libs.junit)

    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.neobortx"
            artifactId = "simplebleclient"
            version = "$versionLib"
            description = "A simple BLE client library for Android that works with coroutines"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

tasks.register("printReleaseArtifactName") {
    doLast {
        println("simpleBleClient_$versionLib.aar")
    }
}

tasks.register("printDebugArtifactName") {
    doLast {
        println("simpleBleClient_debug_$versionLib.aar")
    }
}

// Configure all single-project Dokka tasks at the same time,
// such as dokkaHtml, dokkaJavadoc and dokkaGfm.
tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets.configureEach {
        documentedVisibilities.set(
            setOf(
                Visibility.PUBLIC
            )
        )

        perPackageOption {
            matchingRegex.set(".*internal.*")
            suppress.set(true)
        }
    }
}

tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("html-docs")
}

tasks.named("assemble") {
    finalizedBy(tasks.named("dokkaHtmlJar"))
}

tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}
