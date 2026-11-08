plugins {
    `java-library`
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    id("mod-conventions")
}

neoForge {
    version = property("deps.neoforge") as String

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks {
    processResources {
        exclude("fabric.mod.json", "META-INF/mods.toml", "pack.mcmeta")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    named<Copy>("buildAndCollect") {
        from(jar.flatMap { it.archiveFile })
    }
}
