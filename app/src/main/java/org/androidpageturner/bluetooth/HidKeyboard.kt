// SPDX-License-Identifier: MIT

package org.androidpageturner.bluetooth

/** Serializes short key pulses. Transport and scheduling can be tested without Android. */
class HidKeyboard(
    private val sendReport: (ByteArray) -> Boolean,
    private val scheduler: Scheduler,
    private val onBusyChanged: (Boolean) -> Unit,
    private val onFailure: (releaseFailed: Boolean) -> Unit,
) {
    interface Scheduler {
        fun postDelayed(task: Runnable, delayMillis: Long)
        fun remove(task: Runnable)
    }

    private var report = keyUpReport()
    private var releaseAttempts = 0
    var busy: Boolean = false
        private set
    private val releaseTask = Runnable { release() }

    fun currentReport(): ByteArray = report.copyOf()

    fun sendKey(key: HidKey): Boolean {
        if (busy) return false
        val down = keyDownReport(key)
        if (!sendReport(down)) {
            onFailure(false)
            return false
        }
        report = down
        releaseAttempts = 0
        setBusy(true)
        scheduler.postDelayed(releaseTask, KEY_HOLD_MILLIS)
        return true
    }

    private fun release() {
        if (!busy) return
        if (sendReport(keyUpReport())) {
            report = keyUpReport()
            setBusy(false)
        } else if (++releaseAttempts < MAX_RELEASE_ATTEMPTS) {
            scheduler.postDelayed(releaseTask, RELEASE_RETRY_MILLIS)
        } else {
            // Keep the last report until the manager tears down the failed connection.
            onFailure(true)
        }
    }

    /** Send a final key-up before intentional disconnect; forget it after link loss. */
    fun reset(releaseKey: Boolean): Boolean {
        scheduler.remove(releaseTask)
        val released = !releaseKey || !busy || sendReport(keyUpReport())
        report = keyUpReport()
        setBusy(false)
        return released
    }

    private fun setBusy(value: Boolean) {
        busy = value
        onBusyChanged(value)
    }

    companion object {
        const val KEY_HOLD_MILLIS = 40L
        const val RELEASE_RETRY_MILLIS = 30L
        const val MAX_RELEASE_ATTEMPTS = 3
        private const val FIRST_KEY_OFFSET = 2

        fun keyDownReport(key: HidKey): ByteArray = keyUpReport().apply {
            this[FIRST_KEY_OFFSET] = key.usageId
        }

        fun keyUpReport(): ByteArray = ByteArray(HidDescriptor.INPUT_REPORT_SIZE)
    }
}
