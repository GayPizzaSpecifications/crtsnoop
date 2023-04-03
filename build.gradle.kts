import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application

  kotlin("jvm") version "1.8.10"
  kotlin("plugin.serialization") version "1.8.10"

  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("org.graalvm.buildtools.native") version "0.9.20"
}

repositories {
  mavenCentral()
}

java {
  val javaVersion = JavaVersion.toVersion(17)
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}

dependencies {
  implementation("org.jetbrains.kotlin:kotlin-bom")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
  implementation("org.postgresql:postgresql:42.6.0")
  implementation("com.google.guava:guava:31.1-jre")
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "17"
}

tasks.withType<Wrapper> {
  gradleVersion = "8.0.2"
}

application {
  mainClass.set("gay.pizza.crtsnoop.MainKt")
}

graalvmNative {
  binaries {
    named("main") {
      imageName.set("crtsnoop")
      mainClass.set("gay.pizza.crtsnoop.MainKt")
      sharedLibrary.set(false)
    }
  }
}
