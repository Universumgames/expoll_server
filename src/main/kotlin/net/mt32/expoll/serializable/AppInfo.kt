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
        version = "3.4.0",
        build = 182,
        url = "https://testflight.apple.com/join/OpUycQnW",
        releasedTimestamp = UnixTimestamp.fromDateTimeComponents(2024, 5, 6, 18, 0, 0).toClient()
    ),
    stable = AppInfo(
        version = "3.3.1",
        build = 181,
        url = "https://apps.apple.com/app/expoll/id1639799209",
        releasedTimestamp = UnixTimestamp.fromDateTimeComponents(2024, 5, 5, 2, 0,0).toClient()
    ),
    platformKeys = listOf("ios", "macos")
)