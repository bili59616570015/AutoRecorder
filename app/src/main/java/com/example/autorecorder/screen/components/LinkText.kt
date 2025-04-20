package com.example.autorecorder.screen.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration

@Composable
fun LinkText(modifier: Modifier = Modifier, url: String, text: String = url) {
    val context = LocalContext.current
    val annotatedText = buildAnnotatedString {
        append(text)
        addStyle(
            style = SpanStyle(color = Color.Blue, textDecoration = TextDecoration.Underline),
            start = 0,
            end = text.length
        )
        addStringAnnotation(
            tag = "URL",
            annotation = url,
            start = 0,
            end = text.length
        )
    }

    ClickableText(
        modifier = modifier,
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                    context.startActivity(intent)
                }
        }
    )
}

@Composable
fun RequiredText(modifier: Modifier = Modifier, string: String) {
    Text(
        text = buildAnnotatedString {
            append(string)
            pushStyle(SpanStyle(color = Color.Red))
            append(" *")
            pop()
        },
        modifier = modifier
    )
}