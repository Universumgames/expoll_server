package net.mt32.expoll

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
        }
        client.get("/serverInfo").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }
}
