package com.aitranslator.app

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.abs

class TranslatorAccessibilityService : AccessibilityService() {

    private var overlay: LinearLayout? = null
    private var windowManager: WindowManager? = null
    private var overlayParams: WindowManager.LayoutParams? = null

    private var etButton: TextView? = null
    private var ruButton: TextView? = null

    override fun onServiceConnected() {
        super.onServiceConnected()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 6, 8, 6)
            setBackgroundColor(0xEE222222.toInt())
        }

        val dragHandle = makeButton("⋮⋮")

        val et = makeButton("ET").apply {
            setOnClickListener {
                translateFocusedText("ET")
            }
        }

        val ru = makeButton("RU").apply {
            setOnClickListener {
                translateFocusedText("RU")
            }
        }

        panel.addView(dragHandle)
        panel.addView(et)
        panel.addView(ru)

        etButton = et
        ruButton = ru

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 20
            y = 700
        }

        overlayParams = params

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f

        dragHandle.setOnTouchListener { _, event ->
            val p = overlayParams ?: return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = p.x
                    startY = p.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchX
                    val dy = event.rawY - touchY

                    p.x = startX - dx.toInt()
                    p.y = startY + dy.toInt()

                    windowManager?.updateViewLayout(panel, p)
                    true
                }

                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        windowManager?.addView(panel, params)
        overlay = panel
    }

    private fun makeButton(label: String): TextView {
        return TextView(this).apply {
            text = label
            textSize = 16f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(20, 14, 20, 14)
        }
    }

    private fun translateFocusedText(target: String) {
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

        val button = if (target == "ET") etButton else ruButton
        val normalText = target

        button?.text = "…"

        val started = System.currentTimeMillis()

        thread {
            try {
                val translated = callOpenAI(original, target)
                val elapsed =
                    (System.currentTimeMillis() - started) / 1000.0

                runOnMain {
                    replaceText(node, translated)

                    button?.text = normalText

                    toast(
                        "Готово за %.1f сек".format(elapsed)
                    )
                }

            } catch (e: Exception) {
                runOnMain {
                    button?.text = normalText

                    val msg = e.message ?: "API error"
                    toast("Ошибка: ${msg.take(120)}")
                }
            }
        }
    }

    private fun callOpenAI(
        text: String,
        target: String
    ): String {

        val key = BuildConfig.OPENAI_API_KEY

        if (key.isBlank()) {
            throw IllegalStateException("API key пустой")
        }

        val instructions =
            if (target == "ET") {
                """
                Translate the user's message from Russian into natural,
                modern conversational Estonian.

                Preserve the exact meaning and tone.
                Write the way a native Estonian speaker would naturally
                write in a private chat.

                Do not translate word-for-word when that sounds unnatural.
                Preserve names, nicknames, brands, numbers and emojis.
                Correct obvious source-language typos only when the intended
                meaning is clear.

                Never invent a word or name.
                If an unfamiliar name or term is ambiguous, preserve it
                rather than guessing or creating an Estonian-looking word.

                Return ONLY the final Estonian message.
                No explanations, quotes or comments.
                """.trimIndent()
            } else {
                """
                Translate the user's message from Estonian into natural,
                conversational Russian.

                Preserve the exact meaning, tone, names, nicknames,
                numbers and emojis.

                Translate naturally rather than word-for-word.
                Do not invent meanings for unfamiliar names or terms.

                Return ONLY the final Russian message.
                No explanations, quotes or comments.
                """.trimIndent()
            }

        val connection =
            URL("https://api.openai.com/v1/responses")
                .openConnection() as HttpURLConnection

        connection.requestMethod = "POST"

        connection.setRequestProperty(
            "Authorization",
            "Bearer $key"
        )

        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        connection.connectTimeout = 10000
        connection.readTimeout = 30000
        connection.doOutput = true

        val body = JSONObject().apply {
            put("model", "gpt-5-mini")
            put("instructions", instructions)
            put("input", text)
        }

        connection.outputStream.use {
            it.write(
                body.toString().toByteArray(Charsets.UTF_8)
            )
        }

        val code = connection.responseCode

        val stream =
            if (code in 200..299)
                connection.inputStream
            else
                connection.errorStream

        val response = BufferedReader(
            InputStreamReader(stream)
        ).use {
            it.readText()
        }

        connection.disconnect()

        if (code !in 200..299) {
            throw IllegalStateException(
                "OpenAI HTTP $code"
            )
        }

        val json = JSONObject(response)

        val output =
            json.optJSONArray("output")
                ?: throw IllegalStateException("Нет ответа")

        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content =
                item.optJSONArray("content") ?: continue

            for (j in 0 until content.length()) {
                val part =
                    content.optJSONObject(j) ?: continue

                if (
                    part.optString("type") ==
                    "output_text"
                ) {
                    val result =
                        part.optString("text").trim()

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
                AccessibilityNodeInfo
                    .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }

        val success = node.performAction(
            AccessibilityNodeInfo.ACTION_SET_TEXT,
            args
        )

        if (!success) {
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
            val result =
                findEditableNode(node.getChild(i))

            if (result != null) {
                return result
            }
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

    override fun onAccessibilityEvent(
        event: AccessibilityEvent?
    ) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        overlay?.let {
            windowManager?.removeView(it)
        }

        overlay = null
        super.onDestroy()
    }
}
