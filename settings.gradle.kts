rootProject.name = "quietbox"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}


dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

include(":core")
include(":app")

val hasAndroidSdk = providers.gradleProperty("quietbox.android").orNull?.toBoolean()
    ?: (file("local.properties").let { it.exists() && it.readText().contains("sdk.dir") } ||
        System.getenv("ANDROID_HOME") != null || System.getenv("ANDROID_SDK_ROOT") != null)
if (hasAndroidSdk) include(":androidApp")
include(":proxy")
