plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.kmp.library) apply false
}

val hasAndroidSdk = providers.gradleProperty("quietbox.android").orNull?.toBoolean()
    ?: (rootProject.file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") } ||
        System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null)

if (hasAndroidSdk) pluginManager.apply("com.android.kotlin.multiplatform.library")

kotlin {
    jvmToolchain(17)

    if (hasAndroidSdk) {
        extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "dev.quietbox.app"
            compileSdk = libs.versions.android.compile.get().toInt()
            minSdk = libs.versions.android.min.get().toInt()
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "QuietBoxKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material)
            implementation(libs.compose.backhandler)
            implementation(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
