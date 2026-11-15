package com.signops

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec

class ApproveTest {
    @Test fun signsWhatTheApiChecks() {
        assertEquals("1700000000000.pending", Messages.pending("1700000000000"))
        assertEquals("1700000000000.apr_x.approve.47", Messages.respond("1700000000000", "apr_x", "approve", "47"))
        assertEquals("1700000000000.apr_x.deny.", Messages.respond("1700000000000", "apr_x", "deny", ""))
        assertEquals("1700000000000.token.abc", Messages.pushToken("1700000000000", "abc"))
        val pay = Approve.Payment("1499.00", "INR", "Sharma Stores")
        assertEquals("1700000000000.apr_x.approve.47.pay:INR:1499.00:Sharma Stores", Messages.respond("1700000000000", "apr_x", "approve", "47", pay))
        assertEquals("INR 1,499.00 to Sharma Stores", pay.text)
        assertEquals("INR 1,49,999.00", Approve.Payment("149999.00", "INR", "x").amountText)
        assertEquals("USD 149,999.00", Approve.Payment("149999.00", "USD", "x").amountText)
        assertEquals("-_8", Messages.b64u(byteArrayOf(0xfb.toByte(), 0xff.toByte())))
    }

    @Test fun readsPendingRequests() {
        val out = JSONObject("""{"data":[{"id":"apr_1","context":"Sign in from Chrome on Windows","city":"Pune","browser":null,"options":["23","47","81"],"created_at":"2026-10-22T10:00:00.000Z","expires_at":"2026-10-22T10:02:00.000Z"}]}""")
        val r = Approve.parsePending(out).single()
        assertEquals("apr_1", r.id); assertEquals("Pune", r.city); assertNull(r.browser); assertEquals(listOf("23", "47", "81"), r.options)
    }

    @Test fun knowsSignOpsPushes() {
        assertEquals("apr_9", Approve.requestId(mapOf("signops" to "approve", "approve_id" to "apr_9")))
        assertNull(Approve.requestId(mapOf("approve_id" to "apr_9")))
        assertEquals("msg_1", Push.messageId(mapOf("signops_id" to "msg_1")))
        assertNull(Push.messageId(mapOf("signops_id" to "something")))
    }

    /**
     * The whole Approve round trip against the real API, with a software P-256 key standing in for the Keystore: the same
     * SPKI export, DER signatures and messages the SDK uses. Runs when SIGNOPS_SK and SIGNOPS_PK (test keys of one app) are set.
     */
    @Test fun roundTripAgainstTheApi() {
        val sk = System.getenv("SIGNOPS_SK"); val pk = System.getenv("SIGNOPS_PK")
        assumeTrue("set SIGNOPS_SK and SIGNOPS_PK to run against the API", sk != null && pk != null)
        val base = System.getenv("SIGNOPS_BASE") ?: "https://api.signops.dev"
        val gen = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }
        val pair = gen.generateKeyPair()
        fun sign(m: String) = Messages.b64u(Signature.getInstance("SHA256withECDSA").run { initSign(pair.private); update(m.toByteArray()); sign() })
        fun call(key: String, method: String, path: String, body: JSONObject?, headers: Map<String, String> = emptyMap()): Pair<Int, JSONObject> {
            val c = URL(base + path).openConnection() as HttpURLConnection
            c.requestMethod = method
            c.setRequestProperty("authorization", "Bearer $key")
            headers.forEach(c::setRequestProperty)
            if (body != null) { c.doOutput = true; c.setRequestProperty("content-type", "application/json"); c.outputStream.use { it.write(body.toString().toByteArray()) } }
            val s = c.responseCode
            val t = (if (s < 400) c.inputStream else c.errorStream).bufferedReader().readText()
            return s to JSONObject(t)
        }
        val user = "usr_android_${System.currentTimeMillis()}"
        val token = call(sk, "POST", "/v1/approve/enroll-token", JSONObject().put("user_id", user)).second.getString("token")
        val (es, en) = call(pk, "POST", "/v1/approve/devices/enroll", JSONObject().put("token", token).put("public_key", Messages.b64u(pair.public.encoded))
            .put("push_token", "android-test-$user").put("platform", "android").put("name", "JVM test"))
        assertEquals(200, es)
        val dev = en.getString("device_id")
        val ch = call(sk, "POST", "/v1/approve", JSONObject().put("user_id", user).put("context", "Sign in from Chrome on Windows")).second
        val ts = System.currentTimeMillis().toString()
        val (ps, pend) = call(pk, "GET", "/v1/approve/pending", null, mapOf("signops-device" to dev, "signops-timestamp" to ts, "signops-signature" to sign(Messages.pending(ts))))
        assertEquals(200, ps)
        val r = Approve.parsePending(pend).single { it.id == ch.getString("id") }
        assertTrue(ch.getString("match") in r.options)
        val t2 = System.currentTimeMillis().toString()
        val (as_, ans) = call(pk, "POST", "/v1/approve/${r.id}/respond", JSONObject().put("device_id", dev).put("decision", "approve").put("picked", ch.getString("match"))
            .put("timestamp", t2.toLong()).put("signature", sign(Messages.respond(t2, r.id, "approve", ch.getString("match")))))
        assertEquals(200, as_); assertEquals("approved", ans.getString("status"))
        val t3 = System.currentTimeMillis().toString()
        val newToken = "android-test-new-$user"
        val (ts3, _) = call(pk, "POST", "/v1/approve/devices/push-token", JSONObject().put("device_id", dev).put("push_token", newToken).put("timestamp", t3.toLong()).put("signature", sign(Messages.pushToken(t3, newToken))))
        assertEquals(200, ts3)
        call(sk, "POST", "/v1/approve/devices/$dev/remove", JSONObject())
        call(sk, "DELETE", "/v1/devices/$dev", null)
    }
}
