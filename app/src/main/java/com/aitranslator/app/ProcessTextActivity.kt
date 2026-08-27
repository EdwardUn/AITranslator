package com.aitranslator.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle

class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT ->
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?.toString().orEmpty()

            Intent.ACTION_SEND ->
                intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

            else -> ""
        }

        val readOnly =
            intent.action != Intent.ACTION_PROCESS_TEXT ||
            intent.getBooleanExtra(
                Intent.EXTRA_PROCESS_TEXT_READONLY,
                false
            )

        val russian = text.any {
            it in 'А'..'я' || it == 'Ё' || it == 'ё'
        }

        val result = if (russian) {
            "TEST ET: $text"
        } else {
            "ТЕСТ RU: $text"
        }

        AlertDialog.Builder(this)
            .setTitle("AI Translator")
            .setMessage("$text\n\n→\n\n$result")
            .setPositiveButton(
                if (readOnly) "Скопировать перевод" else "Заменить"
            ) { _, _ ->

                if (!readOnly) {
                    setResult(
                        RESULT_OK,
                        Intent().putExtra(
                            Intent.EXTRA_PROCESS_TEXT,
                            result
                        )
                    )
                } else {
                    val clipboard =
                        getSystemService(CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager

                    clipboard.setPrimaryClip(
                        android.content.ClipData.newPlainText(
                            "AI Translator",
                            result
                        )
                    )
                }

                finish()
            }
            .setNegativeButton("Отмена") { _, _ ->
                finish()
            }
            .setOnCancelListener {
                finish()
            }
            .show()
    }
}
