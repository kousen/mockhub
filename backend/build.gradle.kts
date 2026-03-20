plugins {
    java
    jacoco
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.sonarqube") version "7.2.3.7755"
    id("com.diffplug.spotless") version "7.0.2"
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
        mavenBom("org.springframework.ai:spring-ai-bom:2.0.0-M3")
    }
}

dependencies {
    // Core Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-model-ollama")
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")

    // Security - JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6")

    // Payments
    implementation("com.stripe:stripe-java:28.3.0")

    // Image processing
    implementation("net.coobird:thumbnailator:0.4.20")

    // Data generation
    implementation("net.datafaker:datafaker:2.4.2")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-resttestclient")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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
        property("sonar.issue.ignore.multicriteria", "e1,e2,e3")
        // S1186: Empty method body — JPA requires no-arg constructors on entities
        property("sonar.issue.ignore.multicriteria.e1.ruleKey", "java:S1186")
        property("sonar.issue.ignore.multicriteria.e1.resourceKey", "**/entity/**")
        // S1192: Duplicated string literals in seed data
        property("sonar.issue.ignore.multicriteria.e2.ruleKey", "java:S1192")
        property("sonar.issue.ignore.multicriteria.e2.resourceKey", "**/seed/**")
        // S3776: Cognitive complexity in seed data
        property("sonar.issue.ignore.multicriteria.e3.ruleKey", "java:S3776")
        property("sonar.issue.ignore.multicriteria.e3.resourceKey", "**/seed/**")
    }
}

spotless {
    java {
        googleJavaFormat("1.25.2")
        targetExclude("build/**")
    }
}

checkstyle {
    toolVersion = "10.21.4"
    maxWarnings = 0
    configFile = file("config/checkstyle/checkstyle.xml")
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
    }
}
