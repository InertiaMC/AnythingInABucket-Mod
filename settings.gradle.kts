pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("net.minecraftforge.gradle") version "7.0.29" apply false
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// One node per jar: "<version compiled against>-<loader>". Each jar's range is in stonecutter.properties.toml.
stonecutter {
    create(rootProject) {
        fun jar(version: String, vararg loaders: String) {
            for (loader in loaders) version("$version-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        jar("1.21.1", "fabric", "neoforge", "forge")
        jar("1.21.3", "fabric", "neoforge", "forge")
        jar("1.21.4", "fabric", "neoforge", "forge")
        jar("1.21.5", "fabric", "neoforge", "forge")
        jar("1.21.8", "fabric", "neoforge", "forge")
        jar("1.21.10", "fabric", "neoforge", "forge")
        jar("1.21.11", "fabric", "neoforge", "forge")
        jar("26.1.2", "fabric", "neoforge", "forge")
        jar("26.2", "fabric", "neoforge", "forge")
        jar("26.3", "fabric", "neoforge", "forge")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "AnythingInABucket"
