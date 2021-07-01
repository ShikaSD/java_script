import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.xbib.gradle.plugin.jflex") version "1.4.0"
}

val zipped by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

repositories {
    maven(url = "https://www.jetbrains.com/intellij-repository/releases/")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")
}

sourceSets {
    main {
        jflex {
            include("**/*.flex")
        }
        java {
            srcDir("$buildDir/generated/sources/jflex")
        }
    }
}

tasks.withType<KotlinCompile>() {
    dependsOn("generateJflex")
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("de.jflex:jflex:1.8.2")

    zipped("com.jetbrains.intellij.idea:intellij-core:2021.1.3")
    implementation("it.unimi.dsi:fastutil:8.5.4")

    zipped.incoming.artifacts.forEach {
        api(zipTree(it.file))
    }

    testImplementation("junit:junit:4.13")
}
