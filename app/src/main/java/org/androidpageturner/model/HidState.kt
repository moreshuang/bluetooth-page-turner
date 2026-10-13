// SPDX-License-Identifier: MIT

package org.androidpageturner.model

enum class HidStatus {
    STOPPED, PERMISSION_REQUIRED, UNSUPPORTED, BLUETOOTH_DISABLED, REGISTERING,
    READY, NO_DEVICE_SELECTED, CONNECTING, CONNECTED, DISCONNECTING,
    DISCONNECTED, CONNECTION_FAILED, ERROR,
}

data class PairedDevice(val address: String, val label: String)

data class HidState(
    val status: HidStatus = HidStatus.STOPPED,
    val registered: Boolean = false,
    val devices: List<PairedDevice> = emptyList(),
    val selectedAddress: String? = null,
    val sending: Boolean = false,
    val errorRes: Int? = null,
) {
    val canSelect: Boolean
        get() = status !in setOf(HidStatus.CONNECTING, HidStatus.CONNECTED, HidStatus.DISCONNECTING)
    val canConnect: Boolean
        get() = registered && selectedAddress != null && canSelect
    val canSend: Boolean
        get() = registered && status == HidStatus.CONNECTED && !sending
}
