package com.signops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SignOpsTest {
    @Test fun refusesASecretKey() {
        assertThrows(IllegalArgumentException::class.java) { SignOps.checkKey("sk_live_abc") }
        SignOps.checkKey("pk_test_abc")
    }

    @Test fun readsTheApiErrorShape() {
        val e = SignOps.errorFrom(403, """{"error":{"code":"not_allowed","message":"No Android app is added for passkeys."}}""")
        assertEquals(403, e.status); assertEquals("not_allowed", e.code); assertEquals("No Android app is added for passkeys.", e.message)
    }

    @Test fun survivesABodyThatIsNotJson() {
        val e = SignOps.errorFrom(502, "<html>Bad gateway</html>")
        assertEquals("error", e.code); assertEquals("SignOps answered 502", e.message)
    }
}
