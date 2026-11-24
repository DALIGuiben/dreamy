package com.dreamy.mobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.ui.components.BrandLogo
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.Eyebrow
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Light
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.ui.theme.Pale
import com.dreamy.mobile.ui.theme.White

/** `app/page.tsx`: public landing page. */
@Composable
fun HomeScreen(onLogin: () -> Unit, onPresentation: () -> Unit) {
    Scaffold(containerColor = Pale) { padding ->
        PageColumn(Modifier.padding(padding), footer = false) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(Modifier.width(150.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    PrimaryButton("Se connecter", onLogin, trailingIcon = Icons.AutoMirrored.Filled.ArrowForward)
                }
            }
            Eyebrow("SOS ISIMM · Latino Impact", Modifier.padding(top = 24.dp))
            Text(
                buildAnnotatedString {
                    append("Chaque enfant a son univers. ")
                    withStyle(SpanStyle(color = Cyan)) { append("Chaque événement mérite d’être spécial.") }
                },
                style = MaterialTheme.typography.displaySmall,
                color = Navy,
            )
            Muted(
                "Un espace de confiance pour connaître les envies des enfants, préparer des anniversaires " +
                    "attentionnés et organiser les événements SOS avec leurs mamans.",
            )
            PrimaryButton(
                "Accéder à mon espace",
                onLogin,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            DreamyCard(Modifier.padding(top = 16.dp), contentPadding = 16.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Light, RoundedCornerShape(24.dp))
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.EventAvailable,
                            contentDescription = null,
                            tint = Cyan,
                            modifier = Modifier
                                .background(White, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Des moments à leur image", fontWeight = FontWeight.Bold)
                            Muted("Préparés ensemble, en toute confiance", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Feature(Icons.Outlined.Handshake, "Profils attentionnés", "Envies, intérêts et besoins de confort.")
                    Feature(Icons.Outlined.Mic, "Notes vocales", "Une idée racontée, transcrite puis relue.")
                    Feature(Icons.Outlined.AutoAwesome, "Suggestions utiles", "L’IA propose, les personnes décident.")
                    Feature(Icons.Outlined.EventAvailable, "Événements partagés", "Des projets validés avec les mamans.")
                }
            }
            // The web presentation page is reachable by URL only; the app links to it here.
            TextButton(onClick = onPresentation, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Découvrir la présentation", fontWeight = FontWeight.Bold, color = Navy)
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Icon(icon, contentDescription = null, tint = Cyan)
        Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
        Muted(text, Modifier.padding(top = 2.dp), MaterialTheme.typography.bodyMedium)
    }
}
