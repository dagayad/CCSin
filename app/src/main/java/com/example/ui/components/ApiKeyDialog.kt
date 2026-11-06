package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.settings.ApiKeyStore

/**
 * First-launch prompt: the user pastes their own Gemini API key once.
 * It cannot be dismissed without a key, because AI captions need it.
 */
@Composable
fun ApiKeyDialog(onSaved: () -> Unit) {
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {},
        title = { Text("Gemini API key") },
        text = {
            Column {
                Text(
                    "AI captions need your own Gemini API key. " +
                        "Get one free at aistudio.google.com/apikey, then paste it here. " +
                        "It is saved only on this phone."
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("API key") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                enabled = text.trim().length > 10,
                onClick = {
                    ApiKeyStore.save(context, text)
                    onSaved()
                }
            ) {
                Text("Save")
            }
        }
    )
}
