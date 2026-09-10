package com.github.jpmand.idea.plugin.gitea.api

import org.jetbrains.annotations.NonNls

data class GiteaVersion(
    val major: Int,
    val minor: Int? = null,
    val patch: Int? = null,
    val metadata: String? = null,
    val original: String? = null
) : Comparable<GiteaVersion> {

    /**
     * Compares by major/minor/patch only. The metadata / pre-release segment (e.g. the
     * `+dev-651-gcb08549242` in `1.27.0+dev-651-gcb08549242`, or `-rc0`) is intentionally
     * ignored — this type exists to gate against a lower bound, and treating `1.27.0-rc0` as
     * `1.27.0` is the desired behaviour there.
     */
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
        // Tolerant: optional leading `v`, `-` or `+` before the build/pre-release tail, and
        // trailing junk after the numeric prefix are all accepted. Gitea reports plain
        // `1.26.4`; dev builds `1.27.0+dev-651-gcb08549242`; RCs `1.26.0-rc0`; Forgejo
        // `11.0.1+gitea-1.22.0`.
        private val VERSION_REGEX = Regex("""v?(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:[-+](.+))?""")

        /**
         * Parses a Gitea/Forgejo `/version` string. **Never throws** for a non-blank input: an
         * unrecognisable value yields `GiteaVersion(0)`, which sorts below any real floor, so a
         * genuinely unparseable server is treated as unsupported rather than crashing login.
         */
        fun fromString(version: @NonNls String): GiteaVersion =
            fromStringOrNull(version) ?: GiteaVersion(0, original = version)

        @JvmStatic
        fun fromStringOrNull(version: @NonNls String): GiteaVersion? {
            val match = VERSION_REGEX.find(version.trim()) ?: return null
            val (major, minor, patch, metadata) = match.destructured
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