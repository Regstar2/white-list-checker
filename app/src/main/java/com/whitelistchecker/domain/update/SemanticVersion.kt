package com.whitelistchecker.domain.update

import java.math.BigInteger

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
) : Comparable<SemanticVersion> {

    val isPreRelease: Boolean
        get() = preRelease.isNotEmpty()

    override fun compareTo(other: SemanticVersion): Int {
        compareValues(major, other.major).takeIf { it != 0 }?.let { return it }
        compareValues(minor, other.minor).takeIf { it != 0 }?.let { return it }
        compareValues(patch, other.patch).takeIf { it != 0 }?.let { return it }

        if (!isPreRelease && !other.isPreRelease) return 0
        if (!isPreRelease) return 1
        if (!other.isPreRelease) return -1

        val sharedSize = minOf(preRelease.size, other.preRelease.size)
        for (index in 0 until sharedSize) {
            val comparison = compareIdentifier(preRelease[index], other.preRelease[index])
            if (comparison != 0) return comparison
        }
        return compareValues(preRelease.size, other.preRelease.size)
    }

    companion object {
        private val VERSION_PATTERN = Regex(
            pattern = "^[vV]?(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-([0-9A-Za-z.-]+))?(?:\\+[0-9A-Za-z.-]+)?$",
        )

        fun parse(value: String): SemanticVersion? {
            val match = VERSION_PATTERN.matchEntire(value.trim()) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            val patch = match.groupValues[3].toIntOrNull() ?: return null
            val preRelease = match.groupValues[4]
                .takeIf { it.isNotBlank() }
                ?.split('.')
                ?.takeIf { identifiers -> identifiers.all { it.isNotBlank() } }
                ?: emptyList()

            return SemanticVersion(
                major = major,
                minor = minor,
                patch = patch,
                preRelease = preRelease,
            )
        }

        private fun compareIdentifier(left: String, right: String): Int {
            val leftNumeric = left.all(Char::isDigit)
            val rightNumeric = right.all(Char::isDigit)
            return when {
                leftNumeric && rightNumeric -> BigInteger(left).compareTo(BigInteger(right))
                leftNumeric -> -1
                rightNumeric -> 1
                else -> left.compareTo(right)
            }
        }
    }
}
