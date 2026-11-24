package com.dreamy.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.ui.theme.White

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)

/** `.btn-primary`: cyan pill. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = ButtonPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = Cyan,
            contentColor = White,
            disabledContainerColor = Cyan.copy(alpha = 0.45f),
            disabledContentColor = White,
        ),
    ) { ButtonContent(text, icon, trailingIcon) }
}

/** `.btn-secondary`: white pill with a light border. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        contentPadding = ButtonPadding,
        border = BorderStroke(1.dp, Light),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = White,
            contentColor = Navy,
            disabledContentColor = Navy.copy(alpha = 0.45f),
        ),
    ) { ButtonContent(text, icon, trailingIcon) }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?, trailingIcon: ImageVector?) {
    icon?.let {
        Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, fontWeight = FontWeight.Bold)
    trailingIcon?.let {
        Spacer(Modifier.width(8.dp))
        Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
    }
}
