package com.aitranslator.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val processTextMode =
            intent?.action == Intent.ACTION_PROCESS_TEXT

        val selectedText =
            if (processTextMode) {
                intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                    ?.toString()
                    ?: ""
            } else {
                ""
            }

        val readOnly =
            intent.getBooleanExtra(
                Intent.EXTRA_PROCESS_TEXT_READONLY,
                false
            )

        setContent {
            MaterialTheme {
                ProcessTextScreen(
                    selectedText = selectedText,
                    processTextMode = processTextMode,
                    readOnly = readOnly,
                    onReplace = { newText ->
                        val resultIntent = Intent().apply {
                            putExtra(
                                Intent.EXTRA_PROCESS_TEXT,
                                newText
                            )
                        }

                        setResult(
                            Activity.RESULT_OK,
                            resultIntent
                        )

                        finish()
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun ProcessTextScreen(
    selectedText: String,
    processTextMode: Boolean,
    readOnly: Boolean,
    onReplace: (String) -> Unit,
    onClose: () -> Unit
) {
    var result by remember {
        mutableStateOf("")
    }

    var mode by remember {
        mutableStateOf("AUTO")
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = if (processTextMode)
                    "AI Translator"
                else
                    "AI Translator — настройки",
                style = MaterialTheme.typography.headlineMedium
            )

            if (!processTextMode) {
                Text(
                    "Выдели текст в Messenger, Telegram или другом приложении и выбери AI Translator."
                )

                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Готов к обработке текста")
                }

                return@Column
            }

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Выделенный текст",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(selectedText)
                }
            }

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == "AUTO",
                    onClick = { mode = "AUTO" },
                    label = { Text("Авто") }
                )

                FilterChip(
                    selected = mode == "ET_RU",
                    onClick = { mode = "ET_RU" },
                    label = { Text("ET → RU") }
                )

                FilterChip(
                    selected = mode == "RU_ET",
                    onClick = { mode = "RU_ET" },
                    label = { Text("RU → ET") }
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    result =
                        when (mode) {
                            "ET_RU" ->
                                "ТЕСТ RU: $selectedText"

                            "RU_ET" ->
                                "TEST ET: $selectedText"

                            else ->
                                if (
                                    selectedText.any {
                                        it in 'а'..'я' ||
                                        it in 'А'..'Я'
                                    }
                                ) {
                                    "TEST ET: $selectedText"
                                } else {
                                    "ТЕСТ RU: $selectedText"
                                }
                        }
                }
            ) {
                Text("Перевести")
            }

            if (result.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier =
                            Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Результат",
                            style =
                                MaterialTheme.typography.labelLarge
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(result)
                    }
                }

                if (!readOnly) {
                    Button(
                        modifier =
                            Modifier.fillMaxWidth(),
                        onClick = {
                            onReplace(result)
                        }
                    ) {
                        Text(
                            "Заменить выделенный текст"
                        )
                    }
                }
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClose
            ) {
                Text("Закрыть")
            }
        }
    }
}
