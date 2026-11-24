package com.dreamy.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dreamy.mobile.ui.theme.Coral
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.ui.theme.Pale
import com.dreamy.mobile.ui.theme.White

/** `.card`: white, rounded, light border, soft shadow. Clickable when [onClick] is set. */
@Composable
fun DreamyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    val colors = CardDefaults.cardColors(containerColor = White, contentColor = Navy)
    val border = BorderStroke(1.dp, Light)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    val inner: @Composable ColumnScope.() -> Unit = {
        Column(Modifier.padding(contentPadding), content = content)
    }
    if (onClick != null) {
        Card(onClick, modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, border = border, content = inner)
    } else {
        Card(modifier.fillMaxWidth(), shape = shape, colors = colors, elevation = elevation, border = border, content = inner)
    }
}

/** Pale rounded block used inside cards (`rounded-xl bg-[#EFFAFF] p-3`). */
@Composable
fun PaleBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Pale, RoundedCornerShape(12.dp))
            .padding(12.dp),
        content = content,
    )
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, coral: Boolean = false) {
    Text(
        text,
        modifier = modifier
            .background(if (coral) Coral.copy(alpha = 0.1f) else Light, CircleShape)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = Navy,
    )
}

@Composable
fun PillRow(items: List<String>, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { Pill(it) }
    }
}

/** Emoji avatar on a light rounded square; children have no photos. */
@Composable
fun Avatar(emoji: String?, size: Dp = 48.dp) {
    Box(
        Modifier
            .size(size)
            .background(Light, RoundedCornerShape(size / 3)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji?.takeIf { it.isNotBlank() } ?: "🌟", fontSize = (size.value * 0.45f).sp)
    }
}

@Composable
fun StatCard(icon: ImageVector, value: Int, label: String, modifier: Modifier = Modifier) {
    DreamyCard(modifier, contentPadding = 16.dp) {
        Icon(icon, contentDescription = null, tint = Cyan)
        Text(
            value.toString(),
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Coral,
        )
        Muted(label, style = MaterialTheme.typography.bodyMedium)
    }
}

/** `<Empty>`: centered muted message with an optional call to action. */
@Composable
fun EmptyState(text: String, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    DreamyCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Muted(text)
            if (actionLabel != null && onAction != null) {
                PrimaryButton(actionLabel, onAction, Modifier.padding(top = 16.dp))
            }
        }
    }
}

/** Bulleted list with a bold heading; renders nothing when empty. */
@Composable
fun BulletSection(title: String, items: List<String>) {
    if (items.isEmpty()) return
    Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
    items.forEach { Text("• $it", modifier = Modifier.padding(top = 4.dp)) }
}
