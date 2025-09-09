package net.mt32.expoll.helper

import net.mt32.expoll.ConfigData
import net.mt32.expoll.commons.serializable.VersionDescriptor
import net.mt32.expoll.config
import kotlin.test.Test
import kotlin.test.assertTrue

class VersionTest {

    @Test
    fun testExactVersionMatch() {
        // given
        val version = VersionDescriptor("1.2.3", 100)
        val clientInfo = net.mt32.expoll.commons.serializable.ClientInfo("1.2.3", "100", "android")
        // when
        val result = versionsMatchExact(version, clientInfo)
        // then
        assertTrue(result)
    }

    @Test
    fun testRangeVersionMatch() {
        // given
        val from = VersionDescriptor("1.0.0", 50)
        val to = VersionDescriptor("2.0.0", 150)
        val clientInfo = net.mt32.expoll.commons.serializable.ClientInfo("1.5.0", "100", "ios")
        // when
        val result = versionMatchRange(from, to, clientInfo)
        // then
        assertTrue(result)
    }

    @Test
    fun testVersionCompatibility() {
        // given
        val clientInfo = net.mt32.expoll.commons.serializable.ClientInfo("1.2.0", "75", "web")
        config = ConfigData(
            compatibleVersions = listOf(
                net.mt32.expoll.commons.serializable.CompatibleVersionDescriptor(
                    from = VersionDescriptor("1.0.0", 50),
                    to = VersionDescriptor("1.3.0", 100),
                    platform = "web"
                ),
                net.mt32.expoll.commons.serializable.CompatibleVersionDescriptor(
                    exact = VersionDescriptor("2.0.0", 200),
                    platform = "android"
                )
            )
        )
        // when
        val result = checkVersionCompatibility(clientInfo)
        // then
        assertTrue(result)
    }
}