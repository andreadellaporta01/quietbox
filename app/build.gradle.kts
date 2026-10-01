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

    jvm("desktop")

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
        }
        named("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        named("desktopTest").dependencies {
            implementation(kotlin("test"))
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.quietbox.app.MainKt"
    }
}

tasks.withType<JavaExec>().matching { it.name == "run" }.configureEach {
    listOf("QUIETBOX_ENGINE", "QUIETBOX_PROXY_URL", "QUIETBOX_TOKEN").forEach { key ->
        providers.environmentVariable(key).orNull?.let { environment(key, it) }
    }
}

tasks.named<Test>("desktopTest") {
    systemProperty("stills.dir", rootProject.layout.projectDirectory.dir("docs/stills").asFile.absolutePath)
}
