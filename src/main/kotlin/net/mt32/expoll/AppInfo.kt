package net.mt32.expoll

import net.mt32.expoll.commons.serializable.PlatformInfo
import net.mt32.expoll.helper.defaultJSON
import java.io.File

val iosPlatformInfo: PlatformInfo
    get() {
        if(_iosPlatformInfo != null) return _iosPlatformInfo as PlatformInfo
        _iosPlatformInfo = defaultJSON.decodeFromString(File("config/versionDescriptors/ios.json").readText())
        return _iosPlatformInfo!!
    }

private var _iosPlatformInfo: PlatformInfo? = null