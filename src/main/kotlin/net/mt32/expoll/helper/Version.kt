package net.mt32.expoll.helper

import java.lang.Integer.min

/**
 * Check if a version is greater than another
 * @param {string} version1 the first version
 * @param {string} version2 the second version
 * @return {number} 1 if version1 > version2, -1 if version1 < version2, 0 if version1 == version2
 */
fun compareVersion(version1: String, version2: String): Int {
    val v1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
    val v2 = version2.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in (0 until min(v1.size, v2.size))) {
        if (v1[i] > v2[i]) return 1
        if (v1[i] < v2[i]) return -1
    }
    return 0
}
