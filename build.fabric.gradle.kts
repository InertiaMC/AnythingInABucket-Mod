plugins {
    id("dev.kikugie.loom-back-compat")
    id("mod-conventions")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    // Bundled so a server needs nothing but Fabric API.
    modImplementation("me.lucko:fabric-permissions-api:${property("deps.lucko_permissions")}")
    include("me.lucko:fabric-permissions-api:${property("deps.lucko_permissions")}")
    modImplementation("eu.pb4:common-protection-api:${property("deps.common_protection")}")
    include("eu.pb4:common-protection-api:${property("deps.common_protection")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")
}

tasks {
    processResources {
        exclude("META-INF/neoforge.mods.toml", "META-INF/mods.toml", "pack.mcmeta")
    }

    named<Copy>("buildAndCollect") {
        from(loomx.modJar.flatMap { it.archiveFile })
    }
}
