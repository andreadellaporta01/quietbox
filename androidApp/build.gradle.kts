plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "dev.quietbox.android"
    compileSdk = libs.versions.android.compile.get().toInt()

    defaultConfig {
        applicationId = "dev.quietbox"
        minSdk = libs.versions.android.min.get().toInt()
        targetSdk = libs.versions.android.compile.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "ENGINE", "\"${providers.gradleProperty("quietbox.engine").getOrElse("mock")}\"")
        buildConfigField("String", "PROXY_URL", "\"${providers.gradleProperty("quietbox.proxy").getOrElse("")}\"")
        buildConfigField("String", "TOKEN", "\"${providers.gradleProperty("quietbox.token").getOrElse("")}\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.activity.compose)
}
