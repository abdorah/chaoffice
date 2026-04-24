plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.inventory.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.inventory.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Material 3
    implementation("androidx.compose.material3:material3")

    // Material Icons
    implementation("androidx.compose.material:material-icons-core")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.9.8")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.13.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Core KTX
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // JNA (required by UniFFI generated bindings)
    implementation("net.java.dev.jna:jna:5.14.0@aar")

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("io.kotest:kotest-property:6.1.11")
    testImplementation("io.kotest:kotest-assertions-core:6.1.11")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}

// To build the Rust native libraries, run `./scripts/build-android.sh` from the
// project root BEFORE running the Gradle build. The script cross-compiles
// mobile_ffi for Android targets and generates Kotlin bindings via UniFFI.
// The .so files are placed in app/src/main/jniLibs/ and picked up automatically.
// Task to build Rust native libraries via the build-android.sh script
// tasks.register<Exec>("buildRustNative") {
//     description = "Cross-compile mobile_ffi for Android targets and generate Kotlin bindings"
//     workingDir = rootProject.projectDir.parentFile // project root (parent of android/)
//     commandLine("bash", "scripts/build-android.sh")
// }
// 
// tasks.named("preBuild") {
//     dependsOn("buildRustNative")
// }