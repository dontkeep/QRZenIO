package com.al.qrzen.ui

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.al.qrzen.R

@Composable
fun AlertShowResult(result: String, onDismiss: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isUrl = remember(result) { Patterns.WEB_URL.matcher(result).matches() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Scan Result", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isUrl) {
                    ClickableText(
                        text = AnnotatedString(result, spanStyle = SpanStyle(color = colorResource(R.color.light_blue), textDecoration = TextDecoration.Underline)),
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
                            context.startActivity(intent)
                        }
                    )
                } else {
                    Text(text = result)
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(result))
                    Toast.makeText(context, "Text copied", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy")
                }
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    )
}