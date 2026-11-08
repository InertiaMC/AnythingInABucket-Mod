import dev.kikugie.stonecutter.build.StonecutterBuildExtension

plugins {
    `java-library`
}

val sc = extensions.getByType<StonecutterBuildExtension>()
val modId = sc.properties.get<String>("mod.id")
val modVersion = sc.properties.get<String>("mod.version")
val loader = sc.current.project.substringAfter('-')

// 1.21.x ships Java 21 to players, 26.x ships Java 25.
val requiredJava = if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

group = sc.properties.get<String>("mod.group")
version = "$modVersion+${sc.current.version}"
base.archivesName = "${sc.properties.get<String>("mod.artifact")}-$loader"

repositories {
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:${sc.properties.get<String>("deps.jupiter")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

// The bucket art (the plugin repo's resource pack and look numbers) is bundled into every jar so a client with the mod
// shows it without the pack. Read from a plugin checkout at ../../plugins/AnythingInABucket by default, or from
// -PartPack=<folder or zip> and -PartNumbers=<numbers.txt>.
fun pluginFile(property: String, path: String): File {
    val given = findProperty(property)?.toString()
    val found = rootProject.file(given ?: "../../plugins/AnythingInABucket/$path")
    if (!found.exists()) throw GradleException("The bucket art's $path is missing: $found. Check out " +
        "AnythingInABucket-Plugin next to this repo or pass -P$property=<path>.")
    return found
}
val artPack = pluginFile("artPack", "resourcepack")
val artNumbers = pluginFile("artNumbers", "src/main/resources/numbers.txt")
val artFiles = if (artPack.isDirectory) fileTree(artPack) else zipTree(artPack)

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    withType<Test>().configureEach {
        useJUnitPlatform()
    }

    processResources {
        val metadata = buildMap {
            put("id", modId)
            put("name", sc.properties.get<String>("mod.name"))
            put("version", modVersion)
            put("description", sc.properties.get<String>("mod.description"))
            put("author", sc.properties.get<String>("mod.author"))
            put("minecraft", sc.properties.get<String>("mod.mc_compat"))
            put("java_version", requiredJava.majorVersion)
            // Forge 1.21's Mixin 0.8.5 knows nothing past JAVA_18; JAVA_17 loads on every Forge.
            put("mixin_compatibility", if (loader == "forge") "JAVA_17" else "JAVA_${requiredJava.majorVersion}")
            sc.properties.getOrNull<String>("mod.fabric_loader_min")?.let { put("fabric_loader", it) }
            sc.properties.getOrNull<String>("mod.loader_compat")?.let { put("loader_compat", it) }
        }
        inputs.properties(metadata)
        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml", "*.mixins.json")) {
            expand(metadata)
        }

        if (artPack.isDirectory) inputs.dir(artPack) else inputs.file(artPack)
        inputs.file(artNumbers)
        from(artNumbers)
        // The pack's base targets 26.3. Older jars take their version's overlay on top; before 1.21.4 that is the
        // old-format overlay mc_1_21_1 plus only the base's sprites.
        val oldFormat = sc.current.parsed < "1.21.4"
        val overlay = if (oldFormat) "mc_1_21_1" else "mc_" + sc.current.version.replace('.', '_')
        // Overlay first, so the base's copy of a file it overrides is skipped.
        from(artFiles) {
            include("$overlay/assets/**")
            eachFile { path = path.removePrefix("$overlay/") }
            includeEmptyDirs = false
        }
        from(artFiles) {
            // Before 1.21.4 there are no item definitions and the overlay replaces every model, so only the sprites.
            include(if (oldFormat) "assets/anythinginabucket/textures/**" else "assets/**")
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds this jar and copies it to build/libs/<mod version>/"
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    }
}
