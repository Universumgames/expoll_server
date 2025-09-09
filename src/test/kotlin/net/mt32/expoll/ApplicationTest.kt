package net.mt32.expoll

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.BeforeAll
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun setup() {
            ConfigLoader.load("test")
        }
    }

    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        client.get("serverInfo").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
