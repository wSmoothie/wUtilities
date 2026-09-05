pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.architectury.dev/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.7"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("dev.architectury.loom") version "1.17.491" apply false
    id("dev.architectury.loom-no-remap") version "1.17.491" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        fun fabric(minecraft: String) {
            version("$minecraft-fabric", minecraft).buildscript("build.fabric.gradle.kts")
        }
        fun neoForge(minecraft: String) {
            val script = if (minecraft.startsWith("26.")) "build.neoforge26.gradle.kts"
                else "build.neoforge.gradle.kts"
            version("$minecraft-neoforge", minecraft).buildscript(script)
        }
        fun forge(minecraft: String) {
            val script = if (minecraft.startsWith("26.")) "build.forge26.gradle.kts"
                else "build.forge.gradle.kts"
            version("$minecraft-forge", minecraft).buildscript(script)
        }

        listOf("1.20", "1.20.2", "1.20.3", "1.20.5", "1.21", "1.21.2",
            "1.21.5", "1.21.6", "1.21.9", "1.21.11", "26.1.2", "26.2").forEach(::fabric)
        listOf("1.20.2", "1.20.3", "1.20.5", "1.21", "1.21.2", "1.21.5",
            "1.21.6", "1.21.9", "1.21.11", "26.1.2", "26.2").forEach(::neoForge)
        listOf("1.20.1", "1.21.1", "1.21.11", "26.1.2", "26.2").forEach(::forge)
        vcsVersion = "1.21.11-fabric"
    }
}

include("bukkit")
project(":bukkit").projectDir = file("platform/bukkit")
rootProject.name = "wUtilities"
