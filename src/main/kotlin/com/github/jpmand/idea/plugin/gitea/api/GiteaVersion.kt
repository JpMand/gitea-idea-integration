package com.github.jpmand.idea.plugin.gitea.api

import org.jetbrains.annotations.NonNls

data class GiteaVersion(
    val major: Int,
    val minor: Int? = null,
    val patch: Int? = null,
    val metadata: String? = null,
    val original: String? = null
) : Comparable<GiteaVersion> {

    //1.27.0+dev-651-gcb08549242
    override fun compareTo(other: GiteaVersion): Int =
        major.compareTo(other.major).takeIf { it != 0 } ?:
        (minor ?: 0).compareTo(other.minor ?: 0).takeIf { it != 0 } ?:
        (patch ?: 0).compareTo(other.patch ?: 0)

    override fun toString(): String =
        original ?: ("$major" +
                when (minor) { null -> "" else -> ".$minor" +
                        when (patch) { null -> ""  else -> ".$patch" +
                                when (metadata) { null -> "" else -> "+$metadata" }} })

    companion object {
        fun fromString(version: @NonNls String): GiteaVersion {
            val regex = Regex("""(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:\+(.+))?""")
            val matchResult = regex.matchEntire(version)
                ?: throw IllegalArgumentException("Invalid version format: $version")

            val (major, minor, patch, metadata) = matchResult.destructured
            return GiteaVersion(
                major.toInt(),
                minor.takeIf { it.isNotEmpty() }?.toInt(),
                patch.takeIf { it.isNotEmpty() }?.toInt(),
                metadata.takeIf { it.isNotEmpty() },
                version
            )
        }
    }
}