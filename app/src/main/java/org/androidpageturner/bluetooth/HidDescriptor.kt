// SPDX-License-Identifier: MIT

package org.androidpageturner.bluetooth

/** Independently assembled from HID 1.11 item definitions; no copied sample code. */
object HidDescriptor {
    const val REPORT_ID = 1
    const val BOOT_REPORT_ID = 0
    const val INPUT_REPORT_SIZE = 8
    const val OUTPUT_REPORT_SIZE = 1

    fun keyboard(): ByteArray = intArrayOf(
        0x05, 0x01, // Usage page: Generic Desktop
        0x09, 0x06, // Usage: Keyboard
        0xA1, 0x01, // Application collection
        0x85, REPORT_ID,
        0x05, 0x07, // Keyboard/Keypad usage page
        0x19, 0xE0, 0x29, 0xE7, // Eight modifier usages
        0x15, 0x00, 0x25, 0x01,
        0x75, 0x01, 0x95, 0x08,
        0x81, 0x02, // Input: Data, Variable, Absolute (modifier byte)
        0x75, 0x08, 0x95, 0x01,
        0x81, 0x01, // Input: Constant (reserved byte)
        0x05, 0x08, // LED usage page
        0x19, 0x01, 0x29, 0x05,
        0x75, 0x01, 0x95, 0x05,
        0x91, 0x02, // Output: five keyboard LED bits
        0x75, 0x03, 0x95, 0x01,
        0x91, 0x01, // Output: padding
        0x05, 0x07,
        0x19, 0x00, 0x29, 0x65, // Key usages 0..101
        0x15, 0x00, 0x25, 0x65,
        0x75, 0x08, 0x95, 0x06,
        0x81, 0x00, // Input: six-key array
        0xC0, // End collection
    ).map(Int::toByte).toByteArray()
}
