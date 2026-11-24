package com.dreamy.mobile.ui.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dreamy.mobile.BuildConfig
import com.dreamy.mobile.R
import com.dreamy.mobile.ui.components.BrandLogo
import com.dreamy.mobile.ui.components.DreamyCard
import com.dreamy.mobile.ui.components.PageColumn
import com.dreamy.mobile.ui.components.PageHeader
import com.dreamy.mobile.ui.components.PrimaryButton
import com.dreamy.mobile.ui.components.SecondaryButton
import com.dreamy.mobile.ui.theme.Cyan
import com.dreamy.mobile.ui.theme.Navy
import com.dreamy.mobile.ui.theme.Pale
import kotlinx.coroutines.launch

private val slides = listOf(
    R.drawable.slide_01, R.drawable.slide_02, R.drawable.slide_03, R.drawable.slide_04,
    R.drawable.slide_05, R.drawable.slide_06, R.drawable.slide_07,
)

/** `app/presentation/page.tsx` + `components/presentation-viewer.tsx`. */
@Composable
fun PresentationScreen(onHome: () -> Unit) {
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    Scaffold(containerColor = Pale) { padding ->
        PageColumn(Modifier.padding(padding), footer = false) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BrandLogo(Modifier.width(140.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Navy)
                        Text("Accueil", fontWeight = FontWeight.SemiBold, color = Navy, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            PageHeader(
                eyebrow = "Présentation Dreamy",
                title = "Découvrez notre projet",
                subtitle = "Parcourez les 7 diapositives, puis téléchargez le fichier PowerPoint.",
            )
            // The browser handles the .pptx download, as the web page's link does.
            PrimaryButton(
                "Télécharger la présentation",
                { uriHandler.openUri("${BuildConfig.BASE_URL}presentation/download") },
                icon = Icons.Outlined.Download,
            )
            DreamyCard(contentPadding = 12.dp) {
                HorizontalPager(pager, Modifier.clip(RoundedCornerShape(12.dp))) { page ->
                    AsyncImage(
                        model = slides[page],
                        contentDescription = "Diapositive ${page + 1} sur ${slides.size} de la présentation Dreamy",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SecondaryButton(
                        "Précédente",
                        { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                        enabled = pager.currentPage > 0,
                        icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    )
                    Text("${pager.currentPage + 1} / ${slides.size}", style = MaterialTheme.typography.labelLarge)
                    SecondaryButton(
                        "Suivante",
                        { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                        enabled = pager.currentPage < slides.lastIndex,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    )
                }
                LazyRow(
                    Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(slides) { index, slide ->
                        val active = index == pager.currentPage
                        Column(
                            Modifier
                                .width(112.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    BorderStroke(2.dp, if (active) Cyan else Pale),
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable(onClickLabel = "Afficher la diapositive ${index + 1}") {
                                    scope.launch { pager.animateScrollToPage(index) }
                                },
                        ) {
                            AsyncImage(
                                model = slide,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f),
                            )
                            Text(
                                "${index + 1} / ${slides.size}",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
