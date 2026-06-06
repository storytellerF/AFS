package com.storyteller_f.file_system.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageTest {
    @Test
    fun addAppendsValuesAndReturnsSameMessage() {
        val message = Message("copy")

        val returned = message
            .add(true)
            .add(7)
            .add("done")
            .add(null)

        assertTrue(returned === message)
        assertEquals("copy", message.name)
        assertEquals("true7donenull", message.get())
        assertTrue(message.time > 0)
    }
}
