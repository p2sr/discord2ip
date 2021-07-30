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
    // Discord API - JDA is the only Java API that supports audio receive
    implementation("net.dv8tion:JDA:4.3.0_298") {
        exclude(module = "opus-java")
    }

    // Opus Audio Codec Decoder
    implementation("me.walkerknapp:rapidopus:2.0.0")

    // NDI API
    implementation("me.walkerknapp:devolay:2.0.2")

    // Fast collections library
    implementation("it.unimi.dsi:fastutil:8.5.4")
}
