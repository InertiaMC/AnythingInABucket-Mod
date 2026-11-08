plugins {
    `java-library`
    id("net.minecraftforge.gradle")
    id("mod-conventions")
}

val modId = property("mod.id") as String

minecraft {
    // Forge runs on Mojang's names on 1.21.x, so no reobfuscation. 26.x is unobfuscated.
    if (sc.current.parsed < "26.1") mappings("official", sc.current.version)
}

repositories {
    minecraft.mavenizer(this)
    maven(fg.forgeMaven)
    maven(fg.minecraftLibsMaven)
}

dependencies {
    implementation(minecraft.dependency("net.minecraftforge:forge:${property("deps.forge")}"))
}

tasks {
    processResources {
        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")

        // Forge warns about a mod without pack.mcmeta. From format 82 (1.21.9) it uses min_format/max_format.
        val (low, high) = sc.properties.get<String>("mod.data_pack").split('-').map(String::toInt)
        val formats = when {
            low >= 82 -> "\"min_format\": $low, \"max_format\": $high"
            low == high -> "\"pack_format\": $high"
            else -> "\"pack_format\": $high, \"supported_formats\": [$low, $high]"
        }
        val pack = mapOf("name" to sc.properties.get<String>("mod.name"), "formats" to formats)
        inputs.properties(pack)
        filesMatching("pack.mcmeta") { expand(pack) }
    }

    named<Jar>("jar") {
        manifest.attributes("MixinConfigs" to "$modId.mixins.json")
    }

    named<Copy>("buildAndCollect") {
        from(jar.flatMap { it.archiveFile })
    }
}
