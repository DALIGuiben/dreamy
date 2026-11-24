package com.dreamy.mobile.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dreamy.mobile.domain.model.User
import com.dreamy.mobile.ui.components.BrandLogo
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.DreamyTextField
import com.dreamy.mobile.ui.components.ErrorBanner
import com.dreamy.mobile.ui.components.Muted
import com.dreamy.mobile.ui.components.PageTitle
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.theme.Pale

/** `app/login/page.tsx` + `components/login-form.tsx`. */
@Composable
fun LoginScreen(onHome: () -> Unit, onLoggedIn: (User) -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = Pale) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandLogo(
                Modifier
                    .width(220.dp)
                    .clickable(onClickLabel = "Dreamy, accueil", onClick = onHome),
            )
            PageTitle("Bienvenue", Modifier.padding(top = 24.dp))
            Muted("Connectez-vous à votre espace sécurisé.", Modifier.padding(top = 8.dp, bottom = 32.dp))
            DreamyCard {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    DreamyTextField(
                        value = state.email,
                        onValueChange = viewModel::onEmail,
                        label = "Adresse e-mail",
                        error = state.emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    )
                    DreamyTextField(
                        value = state.password,
                        onValueChange = viewModel::onPassword,
                        label = "Mot de passe",
                        password = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { viewModel.submit(onLoggedIn) }),
                    )
                    ErrorBanner(state.error)
                    PrimaryButton(
                        text = if (state.submitting) "Connexion…" else "Se connecter",
                        onClick = { viewModel.submit(onLoggedIn) },
                        enabled = !state.submitting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
