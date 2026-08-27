package com.aitranslator.app

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView
import android.widget.Toast

class TranslatorAccessibilityService : AccessibilityService() {

    private var overlay: TextView? = null
    private var windowManager: WindowManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val button = TextView(this).apply {
            text = "✨ ET"
            textSize = 16f
            setPadding(28, 20, 28, 20)
            setBackgroundColor(0xEE222222.toInt())
            setTextColor(0xFFFFFFFF.toInt())

            setOnClickListener {
                replaceFocusedText()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 20
        }

        windowManager?.addView(button, params)
        overlay = button
    }

    private fun replaceFocusedText() {
        val root = rootInActiveWindow

        val node =
            root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditableNode(root)

        if (node == null || !node.isEditable) {
            Toast.makeText(
                this,
                "Не найдено активное поле ввода",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val oldText = node.text?.toString().orEmpty()

        if (oldText.isBlank()) {
            Toast.makeText(
                this,
                "Поле пустое",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val newText = "TEST ET: $oldText"

        val args = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }

        val success = node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )

        Toast.makeText(
            this,
            if (success) "Текст заменён ✓" else "Messenger не разрешил замену",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun findEditableNode(
        node: AccessibilityNodeInfo?
    ): AccessibilityNodeInfo? {
        if (node == null) return null

        if (node.isEditable && node.isFocused) {
            return node
        }

        for (i in 0 until node.childCount) {
            val found = findEditableNode(node.getChild(i))
            if (found != null) return found
        }

        return null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        overlay?.let {
            windowManager?.removeView(it)
        }
        overlay = null
        super.onDestroy()
    }
}
