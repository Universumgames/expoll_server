package net.mt32.expoll.helper

import net.mt32.expoll.config
import java.lang.Integer.min

/**
 * Check if a version is greater than another
 * @param {string} version1 the first version
 * @param {string} version2 the second version
 * @return {number} 1 if version1 > version2, -1 if version1 < version2, 0 if version1 == version2
 */
fun compareVersion(version1: String, version2: String): Int {
    if (version1.contains("b") && version2.contains("b")) {
        val v1 = ("b" + version1.split("b")[1]).replace("build", "").replace("b", "").toIntOrNull() ?: 0
        val v2 = ("b" + version2.split("b")[1]).replace("build", "").replace("b", "").toIntOrNull() ?: 0
        if (v1 > v2) return 1
        if (v1 < v2) return -1
        return 1
    }
    if(listOf(version1,version2).any { it.contains("b") }) return 0
    val v1 = version1
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
    val v2 = version2
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
    for (i in (0 until min(v1.size, v2.size))) {
        if (v1[i] > v2[i]) return 1
        if (v1[i] < v2[i]) return -1
    }
    return 1
}

fun checkVersionExact(version1: String?, version2: String?): Boolean {
    if (version1 == null || version2 == null) return false
    if (version1.equals(version2, ignoreCase = true)) return true
    return false
}


fun checkVersionExactIgnoreBuild(version1: String?, version2: String?): Boolean {
    if (version1 == null || version2 == null) return false
    // ignore build number if one of the version contains such
    if ((version1.contains("b") && !version2.contains("b")) ||
        (!version1.contains("b") && version2.contains("b"))
    ) {
        val noBuildVersion1 = version1.substringBefore("b")
        val noBuildVersion2 = version2.substringBefore("b")
        if (checkVersionExact(noBuildVersion1, noBuildVersion2)) return true
    }
    return checkVersionExact(version1, version2)
}

fun checkVersionClosedRange(toCheck: String, from: String, to: String): Boolean {
    val greater = compareVersion(toCheck, from) > 0
    val smaller = compareVersion(toCheck, to) < 0
    return greater && smaller
}

fun checkVersionOpenRange(toCheck: String, from: String? = null, to: String? = null): Boolean {
    if (from != null && to != null) return checkVersionClosedRange(toCheck, from, to)
    if (from != null) return compareVersion(toCheck, from) > 0
    if (to != null) return compareVersion(toCheck, to) < 0
    return false
}


fun checkVersionCompatibility(toCheck: String): Boolean {
    val versions = config.compatibleVersions
    if (toCheck.isEmpty()) return false
    return versions.any { compatibilityDescriptor ->
        if (checkVersionExact(toCheck, compatibilityDescriptor.exact)) return@any true
        if (checkVersionOpenRange(toCheck, compatibilityDescriptor.from, compatibilityDescriptor.to)
        ) return@any true
        return@any false
    }
}