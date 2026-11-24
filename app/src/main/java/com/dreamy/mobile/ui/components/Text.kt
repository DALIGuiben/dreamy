package com.dreamy.mobile.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.ui.theme.EyebrowStyle
import com.dreamy.mobile.ui.theme.NavyMuted

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier = modifier, style = EyebrowStyle)
}

@Composable
fun PageTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.headlineMedium)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge)
}

@Composable
fun Muted(text: String, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodyLarge) {
    Text(text, modifier = modifier, style = style, color = NavyMuted)
}

/** Eyebrow + page title + optional muted subtitle: the header used on every page. */
@Composable
fun PageHeader(eyebrow: String, title: String, subtitle: String? = null) {
    Eyebrow(eyebrow)
    PageTitle(title, modifier = Modifier.padding(top = 6.dp))
    subtitle?.let { Muted(it, modifier = Modifier.padding(top = 6.dp)) }
}
