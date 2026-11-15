package com.signops

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationsTest {
    @Test fun readsButtonsFromPushData() {
        val b = Notifications.buttons(mapOf("signops_buttons" to """[{"id":"track","title":"Track","link":"https://x.in/t"},{"id":"call","title":"Call"}]"""))
        assertEquals(listOf(Triple("track", "Track", "https://x.in/t"), Triple("call", "Call", null)), b)
    }
    @Test fun noButtonsIsEmpty() {
        assertEquals(emptyList<Triple<String, String, String?>>(), Notifications.buttons(mapOf("x" to "y")))
        assertEquals(emptyList<Triple<String, String, String?>>(), Notifications.buttons(mapOf("signops_buttons" to "not json")))
    }
    @Test fun parsesAnInboxPage() {
        val p = Inbox.parse(org.json.JSONObject("""{"data":[{"id":"inb_1","title":"Bill","body":null,"link":null,"image":null,"data":{"a":"1"},"push_id":null,"created_at":"2026-10-25T10:00:00.000Z","read":false}],"unread":1,"next":null}"""))
        assertEquals(1, p.unread); assertEquals("Bill", p.items[0].title); assertEquals(null, p.items[0].body); assertEquals("1", p.items[0].data["a"])
    }
}
