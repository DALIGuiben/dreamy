package com.dreamy.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import com.dreamy.mobile.R
import com.dreamy.mobile.ui.theme.Light

/** Scrollable page body with the standard spacing, plus the demo disclaimer footer shown in the shell. */
@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    footer: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        content()
        if (footer) {
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = Light)
            Text(
                "Données fictives de démonstration · Les suggestions IA requièrent une validation humaine.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
fun BrandLogo(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.dreamy_logo), contentDescription = "Dreamy", modifier = modifier)
}
