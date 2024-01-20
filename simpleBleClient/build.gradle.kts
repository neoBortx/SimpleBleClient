@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.klint)
    alias(libs.plugins.detekt)
    `maven-publish`
}

val versionLib = project.property("VERSION_LIB")

android {

    namespace = "com.bortxapps.simplebleclient"
    compileSdk = 33

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            version = "$versionLib-alpha01-debug"
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
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
                val outputFileName = "simpleBleClient_${output.name}_$versionLib.aar"
                output.outputFileName = outputFileName
            }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.neobortx"
            artifactId = "simplebleclient"
            version = "$versionLib"
            artifact("$buildDir/outputs/aar/simpleBleClient_release_$versionLib.aar")
            description = "A simple BLE client library for Android that works with coroutines"

            pom {
                name = "Simple BLE Client"
                description = "A simple BLE client library for Android that works with coroutines"
                url = "https://github.com/neoBortx/SimpleBleClient"
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("http://www.opensource.org/licenses/mit-license.php")
                    }
                }
            }
        }
        repositories {
            mavenLocal()
            maven {
                url = uri("https://jitpack.io")
            }
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

tasks.register("printReleaseArtifactName") {
    doLast {
        println("simpleBleClient_release_$versionLib.aar")
    }
}

tasks.register("printDebugArtifactName") {
    doLast {
        println("simpleBleClient_debug_$versionLib.aar")
    }
}