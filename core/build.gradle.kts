plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp.library) apply false
}

val hasAndroidSdk = providers.gradleProperty("quietbox.android").orNull?.toBoolean()
    ?: (rootProject.file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") } ||
        System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null)

if (hasAndroidSdk) pluginManager.apply("com.android.kotlin.multiplatform.library")

kotlin {
    jvmToolchain(17)

    jvm()

    if (hasAndroidSdk) {
        extensions.configure<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget>("android") {
            namespace = "dev.quietbox.core"
            compileSdk = libs.versions.android.compile.get().toInt()
            minSdk = libs.versions.android.min.get().toInt()
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            runtimeOnly(libs.logback)
        }
        if (hasAndroidSdk) named("androidMain").dependencies { implementation(libs.ktor.client.okhttp) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
    }
}

tasks.register<JavaExec>("eval") {
    group = "verification"
    description = "Runs the golden set through the pipeline and prints the scoreboard."
    val jvm = kotlin.jvm().compilations.getByName("main")
    dependsOn(jvm.compileTaskProvider)
    classpath = files(jvm.output.allOutputs, jvm.runtimeDependencyFiles)
    mainClass = "dev.quietbox.core.eval.EvalMainKt"
    args = listOfNotNull(providers.gradleProperty("engine").orNull)
    environment("QUIETBOX_PROXY_URL", providers.environmentVariable("QUIETBOX_PROXY_URL").getOrElse(""))
    environment("QUIETBOX_TOKEN", providers.environmentVariable("QUIETBOX_TOKEN").getOrElse(""))
}
