package com.example.ui.extension

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * Componente visual para mostrar el icono de una extensión web.
 * Usa Coil con fallback elegante a un icono vectorial de Material en caso de error o carga.
 */
@Composable
fun ExtensionLogo(
    iconUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    ) {
        if (!iconUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(iconUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(10.dp)),
                loading = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                },
                error = {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(size * 0.55f)
                        )
                    }
                }
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(size * 0.55f)
                )
            }
        }
    }
}
