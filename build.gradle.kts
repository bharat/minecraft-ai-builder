plugins {
    java
}

group = "com.aibuilder"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // OkHttp for calling AI APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson for JSON parsing (included in Paper runtime, but declared for compilation)
    compileOnly("com.google.code.gson:gson:2.11.0")
}

tasks.jar {
    // Include dependencies in the plugin jar (fat jar for okhttp)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// Copy the built jar into the local server's plugins folder
tasks.register<Copy>("deploy") {
    dependsOn(tasks.jar)
    from(tasks.jar.get().archiveFile)
    into(file("server/plugins"))
}
