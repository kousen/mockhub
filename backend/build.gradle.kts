plugins {
    java
    jacoco
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.sonarqube)
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

sonar {
    properties {
        property("sonar.projectKey", "kousen_mockhub")
        property("sonar.organization", "kousen-it-inc")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.coverage.exclusions", listOf(
            "**/seed/**",
            "**/entity/**",
            "**/dto/**",
            "**/config/AiConfig.java",
            "**/MockHubApplication.java"
        ))

        // Issue exclusions — suppress known false positives
        property("sonar.issue.ignore.multicriteria", "e1,e2,e3,e4,e5")
        // S1186: Empty method body — JPA requires no-arg constructors on entities
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "java:S1186")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/entity/**")
        // S1192: Duplicated string literals in seed data
        property("sonar.issue.ignore.multicriteria.e2.ruleKey", "java:S1192")
        property("sonar.issue.ignore.multicriteria.e2.resourceKey", "**/seed/**")
        // S3776: Cognitive complexity in seed data
        property("sonar.issue.ignore.multicriteria.e3.ruleKey", "java:S3776")
        property("sonar.issue.ignore.multicriteria.e3.resourceKey", "**/seed/**")
        // S6218: Record with byte[] — TicketPdfData is internal, never compared/hashed
        property("sonar.issue.ignore.multicriteria.e4.ruleKey", "java:S6218")
        property("sonar.issue.ignore.multicriteria.e4.resourceKey", "**/ticket/dto/**")
        // S6863: Verification endpoint intentionally returns 200 for all cases — validity is in the response body
        property("sonar.issue.ignore.multicriteria.e5.ruleKey", "java:S6863")
        property("sonar.issue.ignore.multicriteria.e5.resourceKey", "**/ticket/controller/TicketVerificationController.java")
    }
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
