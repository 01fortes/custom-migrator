pluginManagement {
    val kotlinVersion: String by settings
    val detektVersion: String by settings
    val spotlessVersion: String by settings
    val protoGradlePluginVersion: String by settings

    val springBootVersion: String by settings
    val springDependencyManagementVersion: String by settings

    repositories {
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("io.gitlab.arturbosch.detekt") version detektVersion
        id("com.diffplug.spotless") version spotlessVersion

        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion apply false
        kotlin("plugin.spring") version kotlinVersion apply false

        id("org.springframework.boot") version springBootVersion apply false
        id("io.spring.dependency-management") version springDependencyManagementVersion apply false
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}