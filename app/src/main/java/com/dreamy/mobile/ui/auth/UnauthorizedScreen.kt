package com.dreamy.mobile.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageTitle
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.theme.Pale

/** `app/unauthorized/page.tsx`: shown when the backend refuses the account's role. */
@Composable
fun UnauthorizedScreen(onBackToLogin: () -> Unit) {
    Scaffold(containerColor = Pale) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            DreamyCard {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    PageTitle("Accès refusé")
                    Muted("Cet espace n’est pas associé à votre compte.", Modifier.padding(top = 12.dp))
                    PrimaryButton("Revenir à la connexion", onBackToLogin, Modifier.padding(top = 24.dp))
                }
            }
        }
    }
}
