// SPDX-License-Identifier: MIT

package org.androidpageturner.bluetooth

/** USB HID Usage Tables, Keyboard/Keypad page (0x07). */
enum class HidKey(val usageId: Byte) {
    RIGHT_ARROW(0x4F),
    LEFT_ARROW(0x50),
    PAGE_UP(0x4B),
    PAGE_DOWN(0x4E),
}
