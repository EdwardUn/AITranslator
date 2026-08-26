package com.aitranslator.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                TranslatorScreen()
            }
        }
    }
}

@Composable
fun TranslatorScreen() {
    var text by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

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
                text = "AI Translator",
                fontSize = 30.sp
            )

            Text(
                text = "Умный перевод для переписки",
                fontSize = 16.sp
            )

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Введите или вставьте текст") }
            )

            Button(
                onClick = { result = "ET → RU: $text" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("🇪🇪  ET → RU")
            }

            Button(
                onClick = { result = "RU → ET: $text" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("🇷🇺  RU → ET")
            }

            OutlinedButton(
                onClick = { result = "Improve ET: $text" },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("✨  Исправить эстонский")
            }

            if (result.isNotBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = result,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}
