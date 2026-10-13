// SPDX-License-Identifier: MIT

package org.androidpageturner.bluetooth

import org.junit.Assert.*
import org.junit.Test

class HidDescriptorTest {
    @Test fun `descriptor describes eight input bytes and one LED output byte`() {
        val descriptor = HidDescriptor.keyboard()
        var cursor = 0
        var size = 0
        var count = 0
        var reportId = 0
        var inputBits = 0
        var outputBits = 0
        var depth = 0
        while (cursor < descriptor.size) {
            val prefix = descriptor[cursor++].toInt() and 0xFF
            assertNotEquals("Long items are not expected", 0xFE, prefix)
            val length = when (prefix and 3) { 3 -> 4; else -> prefix and 3 }
            assertTrue("Truncated HID item", cursor + length <= descriptor.size)
            var value = 0
            repeat(length) { value = value or ((descriptor[cursor++].toInt() and 0xFF) shl (8 * it)) }
            when (prefix and 0xFC) {
                0x74 -> size = value
                0x94 -> count = value
                0x84 -> reportId = value
                0x80 -> { assertEquals(1, reportId); inputBits += size * count }
                0x90 -> { assertEquals(1, reportId); outputBits += size * count }
                0xA0 -> depth++
                0xC0 -> { depth--; assertTrue(depth >= 0) }
            }
        }
        assertEquals(0, depth)
        assertEquals(64, inputBits)
        assertEquals(8, outputBits)
    }

    @Test fun `descriptor returns fresh bytes`() {
        val first = HidDescriptor.keyboard()
        first[0] = 0
        assertEquals(0x05, HidDescriptor.keyboard()[0].toInt())
    }
}
