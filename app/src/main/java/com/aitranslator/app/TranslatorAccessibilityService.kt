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
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

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
                translateFocusedText()
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

    private fun translateFocusedText() {
        val root = rootInActiveWindow
        val node =
            root?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                ?: findEditableNode(root)

        if (node == null || !node.isEditable) {
            toast("Не найдено поле ввода")
            return
        }

        val original = node.text?.toString().orEmpty().trim()

        if (original.isBlank()) {
            toast("Поле пустое")
            return
        }

        overlay?.text = "⏳"

        thread {
            try {
                val translated = callOpenAI(original)

                runOnMain {
                    replaceText(node, translated)
                    overlay?.text = "✨ ET"
                }
            } catch (e: Exception) {
                runOnMain {
                    overlay?.text = "✨ ET"
                    toast("Ошибка: ${e.message ?: "API"}")
                }
            }
        }
    }

    private fun callOpenAI(text: String): String {
        val key = BuildConfig.OPENAI_API_KEY

        if (key.isBlank()) {
            throw IllegalStateException("API key пустой")
        }

        val url = URL("https://api.openai.com/v1/responses")
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.setRequestProperty(
            "Authorization",
            "Bearer $key"
        )
        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )
        connection.doOutput = true

        val body = JSONObject().apply {
            put("model", "gpt-5-mini")
            put(
                "instructions",
                """
                Translate the user's Russian text into natural, modern Estonian.
                Preserve meaning, tone, names, slang, punctuation and emojis.
                Do not translate literally when a more natural Estonian phrasing exists.
                Return only the final Estonian text.
                Do not explain anything.
                """.trimIndent()
            )
            put("input", text)
        }

        connection.outputStream.use {
            it.write(body.toString().toByteArray(Charsets.UTF_8))
        }

        val code = connection.responseCode

        val stream =
            if (code in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val response = BufferedReader(
            InputStreamReader(stream)
        ).use { it.readText() }

        if (code !in 200..299) {
            throw IllegalStateException(
                "OpenAI $code: $response"
            )
        }

        val json = JSONObject(response)

        val output = json.optJSONArray("output")
            ?: throw IllegalStateException("Нет output")

        for (i in 0 until output.length()) {
            val item = output.getJSONObject(i)
            val content = item.optJSONArray("content") ?: continue

            for (j in 0 until content.length()) {
                val part = content.getJSONObject(j)

                if (part.optString("type") == "output_text") {
                    val result = part.optString("text").trim()

                    if (result.isNotBlank()) {
                        return result
                    }
                }
            }
        }

        throw IllegalStateException("Пустой перевод")
    }

    private fun replaceText(
        node: AccessibilityNodeInfo,
        newText: String
    ) {
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

        if (success) {
            toast("Переведено ✓")
        } else {
            toast("Не удалось заменить текст")
        }
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

    private fun runOnMain(block: () -> Unit) {
        mainExecutor.execute(block)
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
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
