package com.dreamy.mobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dreamy.mobile.domain.model.Child
import com.dreamy.mobile.util.age

/** `components/child-profile.tsx`: approved profile fields as pills. */
@Composable
fun ChildProfile(child: Child) {
    DreamyCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(child.avatar, size = 80.dp)
            Column(Modifier.padding(start = 20.dp)) {
                PageTitle(child.firstName)
                Muted("${child.birthDate.age()} ans")
            }
        }
    }
    child.description?.takeIf { it.isNotBlank() }?.let {
        DreamyCard {
            Text("Présentation", style = MaterialTheme.typography.titleMedium)
            Muted(it, Modifier.padding(top = 8.dp))
        }
    }
    listOf(
        "Personnalité" to child.personalityTags,
        "Centres d’intérêt" to child.interests,
        "Activités préférées" to child.favoriteActivities,
        "Couleurs préférées" to child.favoriteColors,
        "Thèmes d’anniversaire" to child.birthdayThemes,
        "Souhaits de cadeaux" to child.giftWishes,
        "Confort" to child.comfortPreferences,
    ).forEach { (title, items) ->
        DreamyCard {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (items.isEmpty()) {
                Muted("À compléter", Modifier.padding(top = 12.dp), MaterialTheme.typography.bodyMedium)
            } else {
                PillRow(items, Modifier.padding(top = 12.dp))
            }
        }
    }
    child.approvedPlanningSummary?.takeIf { it.isNotBlank() }?.let {
        DreamyCard {
            Text("Résumé approuvé pour la planification", style = MaterialTheme.typography.titleMedium)
            Muted(it, Modifier.padding(top = 8.dp))
        }
    }
}
