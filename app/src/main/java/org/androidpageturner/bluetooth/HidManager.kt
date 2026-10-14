// SPDX-License-Identifier: MIT

package org.androidpageturner.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.androidpageturner.R
import org.androidpageturner.model.HidState
import org.androidpageturner.model.HidStatus
import org.androidpageturner.model.PairedDevice

/** Foreground-only HID owner. All state and callbacks are confined to the main looper. */
@SuppressLint("MissingPermission") // Checked at the entry points; revocation is also caught.
class HidManager(context: Context, private val onState: (HidState) -> Unit) {
    private val context = context.applicationContext
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val handler = Handler(Looper.getMainLooper())
    private var proxy: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private var devices = emptyMap<String, BluetoothDevice>()
    private var active = false
    private var generation = 0
    private var timeout: Runnable? = null
    private var reportId = HidDescriptor.REPORT_ID
    private var ledReport = ByteArray(HidDescriptor.OUTPUT_REPORT_SIZE)
    var state = HidState()
        private set

    private val keyboard = HidKeyboard(
        sendReport = ::sendRawReport,
        scheduler = object : HidKeyboard.Scheduler {
            override fun postDelayed(task: Runnable, delayMillis: Long) {
                handler.postDelayed(task, delayMillis)
            }
            override fun remove(task: Runnable) { handler.removeCallbacks(task) }
        },
        onBusyChanged = { update(state.copy(sending = it)) },
        onFailure = { releaseFailed ->
            if (releaseFailed) {
                Log.e(TAG, "Key release failed; closing HID session")
                stop()
                update(state.copy(status = HidStatus.ERROR, errorRes = R.string.error_release))
            } else {
                fail(R.string.error_report)
            }
        },
    )

    fun hasPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    fun start() {
        if (active) return
        if (!hasPermission()) {
            update(state.copy(status = HidStatus.PERMISSION_REQUIRED, errorRes = R.string.error_permission))
            return
        }
        val bluetoothAdapter = adapter
        if (bluetoothAdapter == null) {
            update(state.copy(status = HidStatus.UNSUPPORTED, errorRes = R.string.error_proxy))
            return
        }
        bluetoothOperation {
            if (!bluetoothAdapter.isEnabled) {
                update(state.copy(status = HidStatus.BLUETOOTH_DISABLED, errorRes = null))
                return@bluetoothOperation
            }
            active = true
            val session = ++generation
            refreshDevices()
            if (!active) return@bluetoothOperation
            update(state.copy(status = HidStatus.REGISTERING, errorRes = null))
            val accepted = bluetoothAdapter.getProfileProxy(
                context, serviceListener(session), BluetoothProfile.HID_DEVICE,
            )
            if (!accepted) {
                active = false
                update(state.copy(status = HidStatus.UNSUPPORTED, errorRes = R.string.error_proxy))
            } else {
                armTimeout(SERVICE_TIMEOUT_MILLIS) {
                    stop()
                    update(state.copy(status = HidStatus.ERROR, errorRes = R.string.error_proxy_timeout))
                }
            }
        }
    }

    fun retry() {
        if (state.status == HidStatus.CONNECTED) {
            refreshDevices()
        } else {
            stop()
            start()
        }
    }

    fun refreshDevices() {
        if (!hasPermission()) return
        bluetoothOperation {
            devices = adapter?.bondedDevices.orEmpty().associateBy { it.address }
            val paired = devices.values.map {
                PairedDevice(
                    it.address,
                    context.getString(
                        R.string.device_label,
                        it.name ?: context.getString(R.string.unnamed_device),
                        it.address.takeLast(5),
                    ),
                )
            }.sortedBy { it.label.lowercase() }
            val selection = state.selectedAddress?.takeIf { it in devices }
            update(state.copy(devices = paired, selectedAddress = selection))
        }
    }

    fun selectDevice(address: String?) {
        if (!state.canSelect) return
        val selected = address?.takeIf { it in devices }
        update(state.copy(
            selectedAddress = selected,
            status = if (state.registered) {
                if (selected == null) HidStatus.NO_DEVICE_SELECTED else HidStatus.READY
            } else state.status,
            errorRes = null,
        ))
    }

    fun connect() {
        if (!state.canConnect) return
        val target = devices[state.selectedAddress] ?: return
        bluetoothOperation {
            Log.i(TAG, "Connect device")
            update(state.copy(status = HidStatus.CONNECTING, errorRes = null))
            if (proxy?.connect(target) != true) {
                update(state.copy(status = HidStatus.CONNECTION_FAILED, errorRes = R.string.error_connect))
            } else {
                armTimeout(CONNECTION_TIMEOUT_MILLIS) {
                    // A late CONNECTED callback must not revive this failed attempt.
                    stop()
                    update(state.copy(status = HidStatus.CONNECTION_FAILED, errorRes = R.string.error_connect))
                }
            }
        }
    }

    fun disconnect() {
        val target = connectedDevice ?: return
        bluetoothOperation {
            if (!keyboard.reset(releaseKey = true)) {
                Log.e(TAG, "Final key-up was rejected before disconnect; closing HID session")
                stop()
                fail(R.string.error_release)
                return@bluetoothOperation
            }
            update(state.copy(status = HidStatus.DISCONNECTING, errorRes = null))
            if (proxy?.disconnect(target) != true) {
                update(state.copy(status = HidStatus.CONNECTED, errorRes = R.string.error_disconnect))
            } else {
                armTimeout(CONNECTION_TIMEOUT_MILLIS) {
                    stop()
                    update(state.copy(status = HidStatus.ERROR, errorRes = R.string.error_disconnect))
                }
            }
        }
    }

    fun sendKey(key: HidKey) {
        if (!state.canSend) return
        Log.d(TAG, "Send ${key.name}")
        keyboard.sendKey(key)
    }

    fun stop() {
        active = false
        ++generation // Invalidates callbacks belonging to this session.
        cancelTimeout()
        val oldProxy = proxy
        val oldDevice = connectedDevice
        if (!keyboard.reset(releaseKey = true)) Log.w(TAG, "Final key-up failed during cleanup")
        cleanupOperation {
            if (oldDevice != null && oldProxy?.disconnect(oldDevice) != true) {
                Log.w(TAG, "Disconnect cleanup command rejected")
            }
        }
        cleanupOperation {
            if (oldProxy != null && !oldProxy.unregisterApp()) {
                Log.w(TAG, "Unregister cleanup command rejected")
            }
        }
        if (oldProxy != null) adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, oldProxy)
        proxy = null
        connectedDevice = null
        update(state.copy(status = HidStatus.STOPPED, registered = false, sending = false, errorRes = null))
    }

    private fun serviceListener(session: Int) = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, service: BluetoothProfile) {
            if (!active || session != generation) {
                adapter?.closeProfileProxy(profile, service)
                return
            }
            if (profile != BluetoothProfile.HID_DEVICE || service !is BluetoothHidDevice) return
            cancelTimeout()
            proxy = service
            Log.i(TAG, "HID service available")
            bluetoothOperation {
                val sdp = BluetoothHidDeviceAppSdpSettings(
                    "Bluetooth Page Turner", "Page-turning keyboard", "Android Page Turner",
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD, HidDescriptor.keyboard(),
                )
                Log.i(TAG, "Register HID app")
                val accepted = service.registerApp(sdp, null, null, context.mainExecutor, callback(session))
                Log.i(TAG, "HID registerApp returned $accepted; waiting for status callback")
                // The registration callback determines the app's actual status. Some
                // phone stacks return false while still completing registration.
                armTimeout(SERVICE_TIMEOUT_MILLIS) {
                    Log.w(TAG, "HID registration status callback timed out (request accepted=$accepted)")
                    stop()
                    fail(R.string.error_register_timeout)
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (!active || session != generation || profile != BluetoothProfile.HID_DEVICE) return
            Log.w(TAG, "HID service disconnected")
            stop()
            fail(R.string.error_service)
        }
    }

    private fun callback(session: Int) = object : BluetoothHidDevice.Callback() {
        private fun valid() = active && session == generation

        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            if (!valid()) return
            cancelTimeout()
            if (!registered) {
                val wasRegistered = state.registered
                Log.w(TAG, "HID app unregistered by Android")
                stop()
                fail(if (wasRegistered) R.string.error_unregistered else R.string.error_register)
                return
            }
            Log.i(TAG, "HID app registered")
            update(state.copy(
                registered = true,
                status = if (state.selectedAddress == null) HidStatus.NO_DEVICE_SELECTED else HidStatus.READY,
                errorRes = null,
            ))
            // pluggedDevice alone does not establish connection; query the actual profile state.
            bluetoothOperation {
                proxy?.connectedDevices?.firstOrNull()?.let {
                    onConnectionStateChanged(it, BluetoothProfile.STATE_CONNECTED)
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, newState: Int) {
            if (!valid()) return
            bluetoothOperation {
                val address = device.address
                if (connectedDevice != null && connectedDevice != device) return@bluetoothOperation
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        cancelTimeout()
                        keyboard.reset(releaseKey = false)
                        reportId = HidDescriptor.REPORT_ID
                        ledReport = ByteArray(HidDescriptor.OUTPUT_REPORT_SIZE)
                        connectedDevice = device
                        refreshDevices()
                        if (!active) return@bluetoothOperation
                        Log.i(TAG, "Device connected")
                        update(state.copy(status = HidStatus.CONNECTED, selectedAddress = address, errorRes = null))
                        if (!sendRawReport(HidKeyboard.keyUpReport())) fail(R.string.error_report)
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        if (state.status == HidStatus.CONNECTING && state.selectedAddress != address) return@bluetoothOperation
                        update(state.copy(status = HidStatus.CONNECTING, selectedAddress = address, errorRes = null))
                        armTimeout(CONNECTION_TIMEOUT_MILLIS) {
                            stop()
                            update(state.copy(status = HidStatus.CONNECTION_FAILED, errorRes = R.string.error_connect))
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        // Preserve CONNECTING until failure is known; some stacks pass through
                        // DISCONNECTING after a rejected outgoing connection.
                        if (connectedDevice != null) update(state.copy(status = HidStatus.DISCONNECTING))
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (connectedDevice == null && state.selectedAddress != address) return@bluetoothOperation
                        cancelTimeout()
                        val failed = state.status == HidStatus.CONNECTING
                        connectedDevice = null
                        keyboard.reset(releaseKey = false)
                        Log.i(TAG, "Device disconnected")
                        update(state.copy(
                            status = if (failed) HidStatus.CONNECTION_FAILED else HidStatus.DISCONNECTED,
                            errorRes = if (failed) R.string.error_connect else null,
                        ))
                    }
                }
            }
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (!valid() || device != connectedDevice) return
            bluetoothOperation {
                val data = when (type) {
                    BluetoothHidDevice.REPORT_TYPE_INPUT -> keyboard.currentReport()
                    BluetoothHidDevice.REPORT_TYPE_OUTPUT -> ledReport.copyOf()
                    else -> null
                }
                if (id.toInt() != reportId || data == null) {
                    sendHandshakeError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                } else if (bufferSize != 0 && bufferSize < data.size) {
                    sendHandshakeError(device, BluetoothHidDevice.ERROR_RSP_INVALID_PARAM)
                } else if (proxy?.replyReport(device, type, id, data) != true) {
                    fail(R.string.error_report)
                }
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (!valid() || device != connectedDevice) return
            bluetoothOperation {
                if (type != BluetoothHidDevice.REPORT_TYPE_OUTPUT || id.toInt() != reportId) {
                    sendHandshakeError(device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID)
                } else if (data.size != HidDescriptor.OUTPUT_REPORT_SIZE) {
                    sendHandshakeError(device, BluetoothHidDevice.ERROR_RSP_INVALID_PARAM)
                } else {
                    ledReport = data.copyOf()
                    sendHandshakeError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
                }
            }
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            if (!valid() || device != connectedDevice) return
            if (reportId.toInt() == this@HidManager.reportId && data.size == HidDescriptor.OUTPUT_REPORT_SIZE) {
                ledReport = data.copyOf()
            }
        }

        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            if (!valid() || device != connectedDevice) return
            if (!keyboard.reset(releaseKey = true)) {
                stop()
                fail(R.string.error_release)
                return
            }
            reportId = if (protocol == BluetoothHidDevice.PROTOCOL_BOOT_MODE) {
                HidDescriptor.BOOT_REPORT_ID
            } else HidDescriptor.REPORT_ID
            Log.d(TAG, "Host changed keyboard protocol")
        }

        override fun onVirtualCableUnplug(device: BluetoothDevice) {
            if (!valid() || device != connectedDevice) return
            Log.i(TAG, "Host unplugged virtual cable")
            stop()
        }
    }

    private fun sendRawReport(report: ByteArray): Boolean {
        val target = connectedDevice ?: return false
        if (!hasPermission()) return false
        return try {
            proxy?.sendReport(target, reportId, report) == true
        } catch (error: SecurityException) {
            Log.e(TAG, "Permission revoked while sending report", error)
            false
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Bluetooth unavailable while sending report", error)
            false
        }
    }

    private fun sendHandshakeError(device: BluetoothDevice, result: Byte) {
        if (proxy?.reportError(device, result) != true) fail(R.string.error_report)
    }

    private fun bluetoothOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (error: SecurityException) {
            Log.e(TAG, "Bluetooth permission rejected", error)
            stop()
            update(state.copy(status = HidStatus.PERMISSION_REQUIRED, errorRes = R.string.error_permission))
        } catch (error: IllegalStateException) {
            Log.e(TAG, "Bluetooth operation unavailable", error)
            stop()
            fail(R.string.error_bluetooth)
        }
    }

    private fun cleanupOperation(operation: () -> Unit) {
        try {
            operation()
        } catch (error: SecurityException) {
            Log.w(TAG, "Bluetooth cleanup denied", error)
        } catch (error: IllegalStateException) {
            Log.w(TAG, "Bluetooth cleanup unavailable", error)
        }
    }

    private fun armTimeout(delayMillis: Long, action: () -> Unit) {
        cancelTimeout()
        val session = generation
        timeout = Runnable { if (active && session == generation) action() }
        handler.postDelayed(timeout!!, delayMillis)
    }

    private fun cancelTimeout() {
        timeout?.let(handler::removeCallbacks)
        timeout = null
    }

    private fun fail(messageRes: Int) {
        Log.e(TAG, context.getString(messageRes))
        update(state.copy(
            status = if (state.status == HidStatus.CONNECTED) HidStatus.CONNECTED else HidStatus.ERROR,
            errorRes = messageRes,
        ))
    }

    private fun update(next: HidState) {
        state = next
        onState(next)
    }

    companion object {
        private const val TAG = "HidManager"
        private const val SERVICE_TIMEOUT_MILLIS = 10_000L
        private const val CONNECTION_TIMEOUT_MILLIS = 20_000L
    }
}
