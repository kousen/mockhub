plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.spotless)
    checkstyle
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
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${libs.versions.spring.ai.get()}")
    }
}

dependencies {
    // Core Spring Boot starters
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.actuator)

    // Spring AI
    implementation(libs.spring.ai.starter.anthropic)
    implementation(libs.spring.ai.starter.openai)
    implementation(libs.spring.ai.starter.ollama)
    implementation(libs.spring.ai.starter.mcp.server)

    // Database
    runtimeOnly(libs.postgresql)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.postgresql)

    // Security - OAuth2 Client
    implementation(libs.spring.boot.starter.oauth2client)

    // Security - MCP OAuth2 (Authorization Server + Resource Server)
    implementation(libs.spring.boot.starter.oauth2.authorization.server)
    implementation(libs.spring.boot.starter.oauth2.resource.server)
    implementation(libs.mcp.authorization.server)
    implementation(libs.mcp.server.security)

    // Security - JWT
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // API Documentation
    implementation(libs.springdoc.openapi)

    // Payments
    implementation(libs.stripe)

    // Image processing
    implementation(libs.thumbnailator)

    // PDF generation and QR codes
    implementation(libs.pdfbox)
    implementation(libs.zxing.core)
    implementation(libs.zxing.javase)

    // SMS delivery
    implementation(libs.twilio)

    // Email delivery
    implementation(libs.spring.boot.starter.mail)

    // Data generation
    implementation(libs.datafaker)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.security.test)
    testImplementation(libs.spring.boot.resttestclient)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testRuntimeOnly(libs.junit.platform.launcher)
}

spotless {
    java {
        googleJavaFormat(libs.versions.google.java.format.get())
        targetExclude("build/**")
    }
}

checkstyle {
    toolVersion = libs.versions.checkstyle.get()
    maxWarnings = 0
    configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.withType<Test> {
    useJUnitPlatform {
        if (project.hasProperty("includeTags")) {
            includeTags(project.property("includeTags") as String)
        } else {
            excludeTags("twilio")
        }
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
