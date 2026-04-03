import net.fabricmc.loom.LoomGradlePlugin
import net.fabricmc.loom.LoomNoRemapGradlePlugin
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import java.net.URI

plugins {
    kotlin("jvm") version "2.3.20"
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.4.1"
    id("su.plo.crowdin.plugin") version "1.2.1"
}

val minecraftVersion = stonecutter.current.version.substringBefore('-')
val noMappings = stonecutter.eval(minecraftVersion, ">=26.1")

version = "${rootProject.version}+$minecraftVersion"
base.archivesName.set(rootProject.name)

if (noMappings) {
    apply<LoomNoRemapGradlePlugin>()

    configurations.api.get().extendsFrom(configurations.create("modApi"))
    configurations.implementation.get().extendsFrom(configurations.create("modImplementation"))
    configurations.compileOnly.get().extendsFrom(configurations.create("modCompileOnly"))
    configurations.runtimeOnly.get().extendsFrom(configurations.create("modRuntimeOnly"))
} else {
    apply<LoomGradlePlugin>()
}

val loom = the<LoomGradleExtensionAPI>()

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://repo.plasmoverse.com/releases")
    maven("https://jitpack.io")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

extensions.getByType(LoomGradleExtensionAPI::class.java).apply {
    accessWidenerPath =
        if (noMappings) {
            rootProject.file("src/main/resources/pvaddonflashback.classtweaker")
        } else {
            rootProject.file("src/main/resources/pvaddonflashback.accesswidener")
        }
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    "minecraft"("com.mojang:minecraft:$minecraftVersion")

    if (!noMappings) {
        "mappings"(loom.officialMojangMappings())
    }

    "modImplementation"("net.fabricmc:fabric-loader:0.18.6")

    "modImplementation"("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    "modImplementation"("maven.modrinth:plasmo-voice:${property("deps.plasmo_voice")}")
    compileOnly("su.plo.voice.api:client:2.1.8")
    compileOnly("su.plo.voice.api:server:2.1.8")

    "modImplementation"("maven.modrinth:flashback:${property("deps.flashback")}")
    "modImplementation"("maven.modrinth:modmenu:${property("deps.modmenu")}")
    "modImplementation"("maven.modrinth:cloth-config:${property("deps.cloth_config")}")
}

crowdin {
    url = URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL()
    sourceFileName = "client/flashback.json"
    resourceDir = "assets/pvaddonflashback/lang"
}

@Suppress("UnstableApiUsage")
val runProdClient by tasks.registering(net.fabricmc.loom.task.prod.ClientProductionRunTask::class) {
    group = "fabric"

    mods.from(project.configurations["modImplementation"])

    outputs.upToDateWhen { false }
}

stonecutter {
    replacements.string(current.parsed >= "26.1") {
        replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
        replace("ResourceLocation", "Identifier")
        replace("playS2C", "clientboundPlay")
    }
}

tasks {
    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        if (noMappings) {
            archiveClassifier.set("")
        }

        relocate("kotlin", "su.plo.voice.libs.kotlin")
        relocate("kotlinx.coroutines", "su.plo.voice.libs.kotlinx.coroutines")
        relocate("kotlinx.serialization", "su.plo.voice.libs.kotlinx.serialization")

        if (noMappings) {
            exclude("pvaddonflashback.accesswidener")
        } else {
            exclude("pvaddonflashback.classtweaker")
        }
    }

    if (!noMappings) {
        named<RemapJarTask>("remapJar") {
            dependsOn(shadowJar)
            inputFile.set(shadowJar.get().archiveFile)
        }
    }

    val copyToRoot =
        register<Copy>("copyToRoot") {
            if (!noMappings) {
                from(named<RemapJarTask>("remapJar").get().archiveFile)
            } else {
                from(shadowJar.get().archiveFile)
            }

            into(rootProject.layout.buildDirectory.dir("libs"))
        }

    build {
        finalizedBy(copyToRoot)
    }

    processResources {
        filesMatching(mutableListOf("fabric.mod.json")) {
            expand(
                mutableMapOf(
                    "version" to project.version,
                    "accessWidener" to if (noMappings) "pvaddonflashback.classtweaker" else "pvaddonflashback.accesswidener",
                    "minecraftVersionDependency" to project.property("minecraft_version_dependency"),
                ),
            )
        }
    }
}

java.toolchain.languageVersion.set(
    JavaLanguageVersion.of(
        if (noMappings) {
            25
        } else {
            21
        },
    ),
)
