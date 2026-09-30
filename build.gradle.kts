plugins {
    java
    id("org.springframework.boot") version "3.5.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.gkcontas"
version = "1.2.0"
description = "OpenAPI in depth: grouped APIs, versioning, polymorphism and a contract test"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Brings springdoc plus the Swagger UI webjar. The -api artifact alone serves the
    // spec but no interface, which is a surprise worth avoiding.
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.9.1")

    // No Lombok here: every type in this project is a record or a configuration class,
    // and an unused annotation processor is a build slowdown with nothing to show for it.

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
