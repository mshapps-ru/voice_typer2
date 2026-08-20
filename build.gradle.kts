group = "com.voicetyper"
version = "1.0.0"

repositories {
    mavenCentral()
}

plugins {
    kotlin("jvm") version "2.1.0"
    application
}

application {
    mainClass.set("com.voicetyper.MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.java.dev.jna:jna-platform:5.15.0")
        testImplementation(kotlin("test"))
    testImplementation("org.mockito:mockito-core:5.15.2")
}

tasks.test {
    useJUnitPlatform()
}
