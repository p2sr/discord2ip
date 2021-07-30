plugins {
    java
}

repositories {
    mavenCentral()
    maven {
        name = "dv8tion"
        url = uri("https://m2.dv8tion.net/releases")
    }
}

dependencies {
    // Core discord2ip utilities
    implementation(project(":core"))

    // Logging framework
    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")
    runtimeOnly("org.apache.logging.log4j:log4j-core:2.14.1")
    runtimeOnly("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.0-rc1")

    // Websocket Client API
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    // JSON Parser/Writer
    implementation("com.dslplatform:dsl-json-java8:1.9.8")
    annotationProcessor("com.dslplatform:dsl-json-java8:1.9.8")

    // Discord API - JDA is the only Java API that supports audio receive
    implementation("net.dv8tion:JDA:4.3.0_298") {
        exclude(module = "opus-java")
    }

    // Fast collections library
    implementation("it.unimi.dsi:fastutil:8.5.4")
}
