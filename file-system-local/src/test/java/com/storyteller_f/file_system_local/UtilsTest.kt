package com.storyteller_f.file_system_local

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {

    @Test
    fun testExtractSdPath() {
        assertEquals("/storage/XX44-XX55", extractSdPath("/storage/XX44-XX55/test"))
        assertEquals("/storage/XX44-XX55", extractSdPath("/storage/XX44-XX55/"))
        assertEquals("/storage/XX44-XX55", extractSdPath("/storage/XX44-XX55"))
        assertEquals("/storage/0000-0000", extractSdPath("/storage/0000-0000/Android/data/com.test"))
    }
}
