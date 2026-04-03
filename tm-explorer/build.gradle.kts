plugins {
    java
    application
}

group = "com.mockhub"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

// Pure Jackson 3 — no Jackson 2 on the classpath
val jacksonVersion = "3.0.4"

dependencies {
    // Jackson 3 core
    implementation("tools.jackson.core:jackson-databind:$jacksonVersion")
    implementation("tools.jackson.core:jackson-core:$jacksonVersion")
    // Annotations are still in the old package (shared between Jackson 2 & 3)
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.20")

    // Jackson 3 Java time support (built-in, no separate module needed in 3.x)

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.mockhub.tmexplorer.TmExplorer")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
