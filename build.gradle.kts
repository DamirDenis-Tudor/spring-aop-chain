plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.spring") version "2.0.21"

    id("io.spring.dependency-management") version "1.1.7"
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "io.github.damirdenis-tudor"
version = "0.0.1"
description = "Spring Boot auto-configured library for building type-safe processing chains using AOP and annotations"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.3")
    }
}

dependencies {
    // Spring Boot auto configuration
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("com.fasterxml.jackson.core:jackson-annotations")

    implementation("org.springframework.boot:spring-boot-starter-aop")

    // generate spring configuration metadata
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

allOpen {
    annotation("io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep")
}

kotlin {
    jvmToolchain(17)

    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)

        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()

    pom {
        name.set("Spring AOP Chain")
        description.set(project.description)
        url.set("https://github.com/damirdenis-tudor/spring-aop-chain")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("damirdenis-tudor")
                name.set("Damir Denis Tudor")
                url.set("https://github.com/damirdenis-tudor")
            }
        }

        scm {
            url.set("https://github.com/damirdenis-tudor/spring-aop-chain")
            connection.set("scm:git:git://github.com/damirdenis-tudor/spring-aop-chain.git")
            developerConnection.set("scm:git:ssh://github.com:damirdenis-tudor/spring-aop-chain.git")
        }
    }
}
