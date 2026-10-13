plugins {
    id("com.android.application")
}

android {
    namespace = "org.androidpageturner"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "org.androidpageturner"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        warningsAsErrors = true
        // Upgrade toolchain versions deliberately after compatibility/license review.
        disable += "AndroidGradlePluginVersion"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
