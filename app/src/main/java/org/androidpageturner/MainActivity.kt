// SPDX-License-Identifier: MIT

package org.androidpageturner

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import org.androidpageturner.bluetooth.HidKey
import org.androidpageturner.bluetooth.HidManager
import org.androidpageturner.ui.MainScreen

class MainActivity : Activity() {
    private lateinit var hid: HidManager
    private lateinit var screen: MainScreen
    private var foreground = false
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != BluetoothAdapter.ACTION_STATE_CHANGED || !foreground) return
            // A protected system broadcast; no device identifiers are read or logged.
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_ON -> hid.retry()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hid = HidManager(this) { screen.render(it) }
        screen = MainScreen(
            this,
            onRefresh = ::requestPermissionOrStart,
            onSettings = ::openBluetoothSettings,
            onSelect = hid::selectDevice,
            onConnect = hid::connect,
            onDisconnect = hid::disconnect,
            onPrevious = { hid.sendKey(HidKey.LEFT_ARROW) },
            onNext = { hid.sendKey(HidKey.RIGHT_ARROW) },
        )
        setContentView(screen.root)
        screen.render(hid.state)
    }

    override fun onStart() {
        super.onStart()
        foreground = true
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(bluetoothReceiver, filter)
        }
        hid.start()
    }

    override fun onStop() {
        foreground = false
        unregisterReceiver(bluetoothReceiver)
        hid.stop()
        super.onStop()
    }

    private fun requestPermissionOrStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !hid.hasPermission()) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), PERMISSION_REQUEST)
        } else {
            hid.retry()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST && foreground) {
            // start() re-checks permission, including denial and an empty/cancelled result.
            hid.start()
        }
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (error: ActivityNotFoundException) {
            Log.e("MainActivity", "Bluetooth settings unavailable", error)
            Toast.makeText(this, R.string.error_settings, Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val PERMISSION_REQUEST = 100
    }
}
