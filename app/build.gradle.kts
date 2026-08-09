plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services) apply false
    id("org.jetbrains.dokka")
    id("jacoco")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
} else {
    logger.warn(
        "app/google-services.json fehlt: Firebase-Login und Push sind zur Laufzeit deaktiviert.",
    )
}

android {
    namespace = "com.example.trnberechnung"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.trnberechnung"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val crewspaceBaseUrl =
            providers
                .gradleProperty("CREWSPACE_BASE_URL")
                .orElse("https://example.invalid/")
                .get()
        buildConfigField("String", "CREWSPACE_BASE_URL", "\"$crewspaceBaseUrl\"")

        val geminiApiKey =
            providers
                .gradleProperty("GEMINI_API_KEY")
                .orElse("")
                .get()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

        // Configurable because Google retires Gemini models faster than app releases ship: the
        // previously hardcoded "gemini-2.5-flash" now answers HTTP 404 "no longer available to new
        // users". Override with -PGEMINI_MODEL=... without touching Kotlin.
        val geminiModel =
            providers
                .gradleProperty("GEMINI_MODEL")
                .orElse("gemini-3.6-flash")
                .get()
        buildConfigField("String", "GEMINI_MODEL", "\"$geminiModel\"")
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1,LICENSE.md,LICENSE-notice.md}")
            excludes.add("META-INF/LICENSE.md")
            excludes.add("META-INF/LICENSE-notice.md")
            pickFirsts.add("META-INF/LICENSE.md")
            pickFirsts.add("META-INF/LICENSE-notice.md")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    // Gemini is called over Retrofit (see network/GeminiApiService.kt) rather than through the
    // archived com.google.ai.client.generativeai SDK, which could not express thinkingLevel or a
    // request timeout and pinned older ktor/coroutines versions than this app forces.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation(platform("com.google.firebase:firebase-bom:34.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-installations")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // MapLibre
    implementation("org.maplibre.gl:android-sdk:11.8.0")
    implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.2")

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Unit Testing: JUnit 4 & JUnit 5 (Jupiter)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    // `useJUnitPlatform()` below only discovers tests through a registered engine. Without the
    // vintage engine every JUnit 4 test (the vast majority of this suite) is silently skipped
    // while the build still reports SUCCESS. Do not remove.
    testRuntimeOnly(libs.junit.vintage.engine)

    // MockK, Kotest, Coroutines, Turbine
    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.turbine)

    // AndroidTest
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
    dokkaSourceSets {
        named("main") {
            sourceRoots.from(file("src/main/java"))
            suppress.set(false)
        }
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/databinding/**",
            "**/android/databinding/**",
            "**/BR.class",
            "**/*_MembersInjector.class",
            "**/*_Factory.class",
            "**/*Component*.*",
            "**/*Module*.*",
            "**/composable/**", // Optional: Compose Previews ausschließen
            "**/*$*", // Hilfsklassen ausschließen
        )

    // Pfade für Kotlin & Java Klassen (angepasst an moderne AGP Versionen)
    val kotlinClassesDir =
        "${layout.buildDirectory.get().asFile}/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes"
    val kotlinTree =
        fileTree(kotlinClassesDir) {
            exclude(fileFilter)
        }
    val javaTree =
        fileTree("${layout.buildDirectory.get().asFile}/intermediates/javac/debug/classes") {
            exclude(fileFilter)
        }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(kotlinTree, javaTree))
    executionData.setFrom(
        fileTree(layout.buildDirectory) {
            include(
                "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec",
                "jacoco/testDebugUnitTest.exec",
            )
        },
    )
}
