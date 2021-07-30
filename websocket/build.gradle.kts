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
    implementation(project(":core")) {
        isTransitive = true
    }

    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.2")

    implementation("com.dslplatform:dsl-json-java8:1.9.8")
    annotationProcessor("com.dslplatform:dsl-json-java8:1.9.8")

    // Discord API - JDA is the only Java API that supports audio receive
    implementation("net.dv8tion:JDA:4.3.0_298") {
        exclude(module = "opus-java")
    }

    // Fast collections library
    implementation("it.unimi.dsi:fastutil:8.5.4")
}
