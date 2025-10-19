import java.net.URI

plugins {
    kotlin("jvm") version "2.0.21"
    id("fabric-loom") version "1.11-SNAPSHOT"
    id("com.gradleup.shadow") version "8.3.5"
    id("su.plo.crowdin.plugin") version "1.1.0-SNAPSHOT"
}

val minecraftVersion = stonecutter.current.version.substringBefore('-')

version = "${rootProject.version}+$minecraftVersion"
base.archivesName.set(rootProject.name)

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

loom {
    accessWidenerPath = rootProject.file("src/main/resources/pvaddonflashback.accesswidener")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")

    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.16.10")

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")

    modImplementation("maven.modrinth:plasmo-voice:${property("deps.plasmo_voice")}")
    compileOnly("su.plo.voice.api:client:2.1.6")
    compileOnly("su.plo.voice.api:server:2.1.6")

    modImplementation("maven.modrinth:flashback:${property("deps.flashback")}")
    modImplementation("maven.modrinth:modmenu:${property("deps.modmenu")}")
    modImplementation("maven.modrinth:cloth-config:${property("deps.cloth_config")}")
}

crowdin {
    url = URI.create("https://github.com/plasmoapp/plasmo-voice-crowdin/archive/refs/heads/addons.zip").toURL()
    sourceFileName = "client/flashback.json"
    resourceDir = "assets/pvaddonflashback/lang"
}

@Suppress("UnstableApiUsage")
val runProdClient by tasks.registering(net.fabricmc.loom.task.prod.ClientProductionRunTask::class) {
    group = "fabric"

    mods.from(project.configurations.modImplementation.get())

    outputs.upToDateWhen { false }
}

tasks {
    shadowJar {
        configurations = listOf(project.configurations.shadow.get())

        relocate("kotlin", "su.plo.voice.libs.kotlin")
        relocate("kotlinx.coroutines", "su.plo.voice.libs.kotlinx.coroutines")
        relocate("kotlinx.serialization", "su.plo.voice.libs.kotlinx.serialization")
    }

    remapJar {
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)
    }

    val copyToRoot =
        register<Copy>("copyToRoot") {
            from(remapJar.get().archiveFile)
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
                ),
            )
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}
