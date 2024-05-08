package net.mt32.expoll.serializable

import kotlinx.serialization.Serializable
import net.mt32.expoll.helper.defaultJSON
import net.mt32.expoll.tClientDateTime
import java.io.File

@Serializable
data class PlatformInfo(
    val beta: AppInfo,
    val stable: AppInfo? = null,
    val platformKeys: List<String>,
)

@Serializable
data class AppInfo(
    val version: String,
    val build: Int,
    val url: String,
    val releasedTimestamp: tClientDateTime
)

val iosPlatformInfo: PlatformInfo
    get() {
        if(_iosPlatformInfo != null) return _iosPlatformInfo as PlatformInfo
        _iosPlatformInfo = defaultJSON.decodeFromString(File("config/versionDescriptors/ios.json").readText())
        return _iosPlatformInfo!!
    }

private var _iosPlatformInfo: PlatformInfo? = null