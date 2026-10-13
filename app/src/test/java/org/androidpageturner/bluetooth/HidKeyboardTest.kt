// SPDX-License-Identifier: MIT

package org.androidpageturner.bluetooth

import org.junit.Assert.*
import org.junit.Test

class HidKeyboardTest {
    private class FakeScheduler : HidKeyboard.Scheduler {
        val pending = mutableListOf<Pair<Runnable, Long>>()
        override fun postDelayed(task: Runnable, delayMillis: Long) { pending += task to delayMillis }
        override fun remove(task: Runnable) { pending.removeAll { it.first == task } }
        fun runNext() { pending.removeAt(0).first.run() }
    }

    private class Fixture {
        val scheduler = FakeScheduler()
        val reports = mutableListOf<ByteArray>()
        val failures = mutableListOf<Boolean>()
        var accepts = true
        val keyboard = HidKeyboard(
            sendReport = { reports += it.copyOf(); accepts },
            scheduler = scheduler,
            onBusyChanged = {},
            onFailure = { failures += it },
        )
    }

    @Test fun `keys have the standardized USB keyboard usages`() {
        assertEquals(0x50, HidKey.LEFT_ARROW.usageId.toInt())
        assertEquals(0x4F, HidKey.RIGHT_ARROW.usageId.toInt())
        assertEquals(0x4B, HidKey.PAGE_UP.usageId.toInt())
        assertEquals(0x4E, HidKey.PAGE_DOWN.usageId.toInt())
    }

    @Test fun `key report contains no report ID and no modifiers`() {
        for (key in HidKey.entries) {
            assertArrayEquals(byteArrayOf(0, 0, key.usageId, 0, 0, 0, 0, 0),
                HidKeyboard.keyDownReport(key))
        }
    }

    @Test fun `key up is an independent all-zero report`() {
        val report = HidKeyboard.keyUpReport()
        report[2] = HidKey.LEFT_ARROW.usageId
        assertArrayEquals(ByteArray(8), HidKeyboard.keyUpReport())
    }

    @Test fun `one pulse sends down then up after a short delay`() {
        val f = Fixture()
        assertTrue(f.keyboard.sendKey(HidKey.RIGHT_ARROW))
        assertTrue(f.keyboard.busy)
        assertEquals(HidKeyboard.KEY_HOLD_MILLIS, f.scheduler.pending.single().second)
        assertArrayEquals(HidKeyboard.keyDownReport(HidKey.RIGHT_ARROW), f.reports.single())
        f.scheduler.runNext()
        assertArrayEquals(ByteArray(8), f.reports.last())
        assertEquals(2, f.reports.size)
        assertFalse(f.keyboard.busy)
        assertArrayEquals(ByteArray(8), f.keyboard.currentReport())
    }

    @Test fun `overlapping presses do not replace a held key`() {
        val f = Fixture()
        f.keyboard.sendKey(HidKey.LEFT_ARROW)
        assertFalse(f.keyboard.sendKey(HidKey.RIGHT_ARROW))
        assertEquals(1, f.reports.size)
        f.scheduler.runNext()
        assertTrue(f.keyboard.sendKey(HidKey.RIGHT_ARROW))
    }

    @Test fun `rejected down does not schedule a release or hold a key`() {
        val f = Fixture()
        f.accepts = false
        assertFalse(f.keyboard.sendKey(HidKey.LEFT_ARROW))
        assertFalse(f.keyboard.busy)
        assertTrue(f.scheduler.pending.isEmpty())
        assertEquals(listOf(false), f.failures)
    }

    @Test fun `release is retried and can recover`() {
        val f = Fixture()
        f.keyboard.sendKey(HidKey.PAGE_DOWN)
        f.accepts = false
        f.scheduler.runNext()
        assertTrue(f.keyboard.busy)
        assertEquals(HidKeyboard.RELEASE_RETRY_MILLIS, f.scheduler.pending.single().second)
        f.accepts = true
        f.scheduler.runNext()
        assertFalse(f.keyboard.busy)
        assertTrue(f.failures.isEmpty())
    }

    @Test fun `persistent release failure tells the manager to tear down the link`() {
        val f = Fixture()
        f.keyboard.sendKey(HidKey.LEFT_ARROW)
        f.accepts = false
        repeat(HidKeyboard.MAX_RELEASE_ATTEMPTS) { f.scheduler.runNext() }
        assertEquals(listOf(true), f.failures)
        assertEquals(1 + HidKeyboard.MAX_RELEASE_ATTEMPTS, f.reports.size)
        assertTrue(f.scheduler.pending.isEmpty())
    }

    @Test fun `intentional cleanup releases the key and cancels pending callbacks`() {
        val f = Fixture()
        f.keyboard.sendKey(HidKey.RIGHT_ARROW)
        assertTrue(f.keyboard.reset(releaseKey = true))
        assertArrayEquals(ByteArray(8), f.reports.last())
        assertFalse(f.keyboard.busy)
        assertTrue(f.scheduler.pending.isEmpty())
    }

    @Test fun `link loss clears local state without attempting an obsolete report`() {
        val f = Fixture()
        f.keyboard.sendKey(HidKey.RIGHT_ARROW)
        f.keyboard.reset(releaseKey = false)
        assertEquals(1, f.reports.size)
        assertArrayEquals(ByteArray(8), f.keyboard.currentReport())
        assertTrue(f.scheduler.pending.isEmpty())
    }

    @Test fun `GET REPORT cannot mutate the active report`() {
        val f = Fixture()
        f.keyboard.sendKey(HidKey.LEFT_ARROW)
        f.keyboard.currentReport()[2] = 0
        assertArrayEquals(HidKeyboard.keyDownReport(HidKey.LEFT_ARROW), f.keyboard.currentReport())
    }
}
