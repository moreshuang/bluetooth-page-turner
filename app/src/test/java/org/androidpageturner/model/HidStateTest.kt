// SPDX-License-Identifier: MIT

package org.androidpageturner.model

import org.junit.Assert.*
import org.junit.Test

class HidStateTest {
    @Test fun `page buttons require a registered connected idle keyboard`() {
        for (status in HidStatus.entries) {
            val state = HidState(status = status, registered = true)
            assertEquals(status == HidStatus.CONNECTED, state.canSend)
            assertFalse(state.copy(sending = true).canSend)
            assertFalse(state.copy(registered = false).canSend)
        }
    }

    @Test fun `connection requires registration selection and no active connection`() {
        val state = HidState(status = HidStatus.READY, registered = true, selectedAddress = "test-device")
        assertTrue(state.canConnect)
        assertFalse(state.copy(selectedAddress = null).canConnect)
        assertFalse(state.copy(registered = false).canConnect)
        for (status in listOf(HidStatus.CONNECTING, HidStatus.CONNECTED, HidStatus.DISCONNECTING)) {
            assertFalse(state.copy(status = status).canConnect)
            assertFalse(state.copy(status = status).canSelect)
        }
    }
}
