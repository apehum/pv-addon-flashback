package su.plo.voice.flashback

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.semver4j.Semver
import org.semver4j.range.RangeListFactory
import java.net.URI

object VersionResolver {
    private const val VERSION_MANIFEST_URL = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"

    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val manifest: VersionManifest by lazy {
        json.decodeFromString<VersionManifest>(
            URI.create(VERSION_MANIFEST_URL).toURL().readText(),
        )
    }

    fun getMinecraftVersionsInRange(
        type: String,
        versionRange: String,
    ): List<Version> {
        val range = RangeListFactory.create(versionRange)

        return manifest.versions
            .filter { it.type == type }
            .mapNotNull { version ->
                Semver.coerce(version.id)?.let { it to version }
            }.filter { range.isSatisfiedBy(it.first) }
            .sortedBy { it.first }
            .map { it.second }
    }

    @Serializable
    data class VersionManifest(
        val versions: List<Version>,
    )

    @Serializable
    data class Version(
        val id: String,
        val type: String,
    )
}
