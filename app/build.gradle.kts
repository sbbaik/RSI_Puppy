plugins {
    // 중복된 id() 호출을 삭제하고 alias 방식으로 통일합니다.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    // Room Annotation Processing을 위한 kapt 추가
    id("kotlin-kapt")
}

android {
    namespace = "com.example.rsi_puppy"
    compileSdk = 36 // 참고: 현재 안정 버전은 34~35이며, 36은 최신 프리뷰일 수 있습니다.

    defaultConfig {
        applicationId = "com.example.rsi_puppy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "TWELVE_DATA_API_KEY",
            "\"e52e488754c649b19b89f65371946328\""
        )
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
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // --- Room Database (새로 추가됨) ---
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // --- Core & Compose ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.01.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.01.01"))

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")

    // Icons & Badge
    implementation("androidx.compose.material:material-icons-extended")
    implementation("me.leolin:ShortcutBadger:1.1.22@aar")

    // Reorderable
    implementation("sh.calvin.reorderable:reorderable-android:3.0.0")

    // Debug / Test
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Coroutines / WorkManager / Network
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")

    // RSI 계산 & DataStore
    implementation("org.ta4j:ta4j-core:0.16")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}