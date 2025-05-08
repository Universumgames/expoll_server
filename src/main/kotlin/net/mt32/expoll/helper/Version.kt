package net.mt32.expoll.helper

import net.mt32.expoll.commons.serializable.ClientInfo
import net.mt32.expoll.commons.serializable.VersionDescriptor
import net.mt32.expoll.config
import java.lang.Integer.min

/**
 * Check if a version is greater than another
 * @param {string} version1 the first version
 * @param {string} version2 the second version
 * @return {number} 1 if version1 > version2, -1 if version1 < version2, 0 if version1 == version2
 */
fun compareVersionString(version1: String, version2: String): Int {
    val v1 = version1
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
    val v2 = version2
        .split(".")
        .map { it.toIntOrNull() ?: 0 }
    for (i in (0 until min(v1.size, v2.size))) {
        if (v1[i] > v2[i])
            return 1
        if (v1[i] < v2[i])
            return -1
    }
    return 0
}

/**
 * Check if a version is greater than another
 * @return {number} 1 if toCheck > version, -1 if toCheck < version, 0 if toCheck == version
 */
fun compareVersion(version: VersionDescriptor, toCheck: ClientInfo): Int {
    if (version.version != toCheck.version) return compareVersionString(toCheck.version, version.version)
    if (version.build == null || toCheck.build == null) return 0
    if (version.build!!.toInt() > toCheck.build!!.toInt()) return -1
    if (version.build!!.toInt() < toCheck.build!!.toInt()) return 1
    return 0
}

fun versionsMatchExact(version1: VersionDescriptor, version2: ClientInfo): Boolean {
    return compareVersion(version1, version2) == 0
}

fun versionMatchRange(from: VersionDescriptor?, to: VersionDescriptor?, toCheck: ClientInfo): Boolean {
    if (from != null && to != null)
        return compareVersion(from, toCheck) >= 0 && compareVersion(to, toCheck) <= 0
    if (from != null)
        return compareVersion(from, toCheck) >= 0
    if (to != null)
        return compareVersion(to, toCheck) <= 0
    return false
}

fun checkVersionCompatibility(toCheck: ClientInfo): Boolean {
    val versions = config.compatibleVersions
    return versions.any { compatibilityDescriptor ->
        if (compatibilityDescriptor.platform != toCheck.platform)
            return@any false
        if (compatibilityDescriptor.exact != null &&
            versionsMatchExact(compatibilityDescriptor.exact!!, toCheck)
        )
            return@any true
        if (versionMatchRange(
                compatibilityDescriptor.from,
                compatibilityDescriptor.to,
                toCheck
            )
        ) return@any true
        return@any false
    }
}