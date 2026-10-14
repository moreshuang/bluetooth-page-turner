// SPDX-License-Identifier: MIT

package org.androidpageturner.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import org.androidpageturner.R
import org.androidpageturner.model.HidState
import org.androidpageturner.model.HidStatus
import org.androidpageturner.model.PairedDevice

/** Plain framework views keep the initial app small and dependency-free apart from Kotlin. */
class MainScreen(
    private val context: Context,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onSelect: (String?) -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    val root = ScrollView(context).apply { isFillViewport = true }
    private val content = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
    private val status = TextView(context)
    private val error = TextView(context)
    private val devicePicker = Spinner(context)
    private val connect: Button
    private val disconnect: Button
    private val previous: Button
    private val next: Button
    private var listedDevices = emptyList<PairedDevice>()
    private var selectedPosition = 0

    init {
        root.addView(content)
        val padding = dp(20)
        content.setPadding(padding, padding, padding, padding)
        // Handles enforced edge-to-edge on targetSdk 35+ and legacy system bars.
        root.setOnApplyWindowInsetsListener { _, insets ->
            @Suppress("DEPRECATION")
            content.setPadding(
                padding + insets.systemWindowInsetLeft, padding + insets.systemWindowInsetTop,
                padding + insets.systemWindowInsetRight, padding + insets.systemWindowInsetBottom,
            )
            @Suppress("DEPRECATION")
            insets.consumeSystemWindowInsets()
        }
        text(R.string.app_name, 26f)
        status.textSize = 20f
        status.setTypeface(null, Typeface.BOLD)
        add(status)
        error.textSize = 16f
        add(error)
        text(R.string.device_heading, 18f)
        devicePicker.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
            listOf(context.getString(R.string.no_device)))
        devicePicker.minimumHeight = dp(56)
        add(devicePicker)
        devicePicker.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // setSelection/adapter changes also trigger this callback, sometimes asynchronously.
                if (position == selectedPosition) return
                selectedPosition = position
                onSelect(listedDevices.getOrNull(position - 1)?.address)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        val connectionActions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        connect = button(R.string.connect, onConnect)
        disconnect = button(R.string.disconnect, onDisconnect)
        connectionActions.addView(connect, LinearLayout.LayoutParams(0, dp(56), 1f).apply {
            marginEnd = dp(8)
        })
        connectionActions.addView(disconnect, LinearLayout.LayoutParams(0, dp(56), 1f))
        add(connectionActions)

        text(R.string.page_controls, 18f)
        val pageActions = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
        previous = pageButton(R.string.previous_large, R.string.previous_description,
            Color.rgb(40, 83, 172), onPrevious)
        next = pageButton(R.string.next_large, R.string.next_description,
            Color.rgb(18, 112, 65), onNext)
        pageActions.addView(previous, LinearLayout.LayoutParams(0, dp(200), 1f).apply {
            marginEnd = dp(12)
        })
        pageActions.addView(next, LinearLayout.LayoutParams(0, dp(200), 1f))
        add(pageActions)

        add(button(R.string.retry, onRefresh))
        add(button(R.string.settings, onSettings))
        text(R.string.foreground_hint, 14f)
    }

    fun render(state: HidState) {
        status.setText(when (state.status) {
            HidStatus.STOPPED -> R.string.status_stopped
            HidStatus.PERMISSION_REQUIRED -> R.string.status_permission
            HidStatus.UNSUPPORTED -> R.string.status_unsupported
            HidStatus.BLUETOOTH_DISABLED -> R.string.status_disabled
            HidStatus.REGISTERING -> R.string.status_registering
            HidStatus.READY -> R.string.status_ready
            HidStatus.NO_DEVICE_SELECTED -> R.string.status_no_device
            HidStatus.CONNECTING -> R.string.status_connecting
            HidStatus.CONNECTED -> R.string.status_connected
            HidStatus.DISCONNECTING -> R.string.status_disconnecting
            HidStatus.DISCONNECTED -> R.string.status_disconnected
            HidStatus.CONNECTION_FAILED -> R.string.status_connection_failed
            HidStatus.ERROR -> R.string.status_error
        })
        error.visibility = if (state.errorRes == null) View.GONE else View.VISIBLE
        state.errorRes?.let(error::setText)
        selectedPosition = state.devices.indexOfFirst { it.address == state.selectedAddress } + 1
        if (listedDevices != state.devices) {
            listedDevices = state.devices
            devicePicker.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item,
                listOf(context.getString(R.string.no_device)) + listedDevices.map { it.label })
        }
        devicePicker.setSelection(selectedPosition)
        devicePicker.isEnabled = state.canSelect
        connect.isEnabled = state.canConnect
        disconnect.isEnabled = state.status == HidStatus.CONNECTED
        previous.isEnabled = state.canSend
        next.isEnabled = state.canSend
    }

    private fun text(resource: Int, size: Float) {
        add(TextView(context).apply { setText(resource); textSize = size })
    }

    private fun button(resource: Int, action: () -> Unit): Button = Button(context).apply {
        setText(resource)
        isAllCaps = false
        setOnClickListener { action() }
    }

    private fun pageButton(label: Int, description: Int, color: Int, action: () -> Unit): Button =
        button(label, action).apply {
            contentDescription = context.getString(description)
            gravity = Gravity.CENTER
            textSize = 24f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(8), dp(12), dp(8), dp(12))
            backgroundTintList = ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(Color.rgb(215, 220, 217), color),
            )
            setTextColor(ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(Color.rgb(83, 91, 86), Color.WHITE),
            ))
        }

    private fun add(view: View) {
        content.addView(view, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(12) })
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
