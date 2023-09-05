package net.mt32.expoll.serializable

import kotlinx.serialization.Serializable
import net.mt32.expoll.helper.UnixTimestamp
import net.mt32.expoll.tClientDateTime

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

val iosPlatformInfo = PlatformInfo(
    beta = AppInfo(
        version = "2.6.0",
        build = 79,
        url = "https://testflight.apple.com/join/OpUycQnW",
        releasedTimestamp = UnixTimestamp.fromDateTimeComponents(2023, 9, 4, 0, 5, 0).toClient()
    ),
    stable = AppInfo(
        version = "2.6.0",
        build = 90,
        url = "https://apps.apple.com/app/expoll/id1639799209",
        releasedTimestamp = UnixTimestamp.fromDateTimeComponents(2023, 10, 1, 0, 0,0).toClient()
    ),
    platformKeys = listOf("ios", "macos")
)