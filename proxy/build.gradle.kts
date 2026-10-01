plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.anthropic.java)
    implementation(libs.logback)
}

application {
    mainClass = "dev.quietbox.proxy.ProxyKt"
}

tasks.named<JavaExec>("run") {
    listOf("ANTHROPIC_API_KEY", "ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_BASE_URL", "QUIETBOX_TOKEN", "QUIETBOX_MODEL", "QUIETBOX_FALLBACKS", "PORT")
        .forEach { key -> providers.environmentVariable(key).orNull?.let { environment(key, it) } }
}
