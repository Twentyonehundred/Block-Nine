package com.chrissmith.blocknine.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

/**
 * The header's account control. Signed out it's a person silhouette ringed in the accent
 * colour, which reads as "sign in"; signed in it becomes the Google profile photo. Either
 * way tapping it opens the leaderboard, where the actual sign-in button lives.
 */
@Composable
fun AccountButton(
    photoUrl: String?,
    colors: BoardColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.boxShaded)
            .border(width = 2.dp, color = colors.accent, shape = CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Leaderboard",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
            )
        } else {
            PersonGlyph(colors.textMuted)
        }
    }
}

/**
 * Head-and-shoulders silhouette. Drawn rather than pulled from an icon pack so the app keeps
 * its single hand-drawn style and doesn't gain a dependency for one shape.
 */
@Composable
private fun PersonGlyph(tint: Color) {
    Canvas(Modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val headRadius = w * 0.20f

        drawCircle(
            color = tint,
            radius = headRadius,
            center = Offset(w / 2f, h * 0.32f),
        )

        // Shoulders: a half-disc clipped by the glyph box, so it reads as a bust.
        val shoulderWidth = w * 0.72f
        val shoulderTop = h * 0.60f
        drawPath(
            path = Path().apply {
                addArc(
                    oval = androidx.compose.ui.geometry.Rect(
                        offset = Offset((w - shoulderWidth) / 2f, shoulderTop),
                        size = Size(shoulderWidth, shoulderWidth),
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                )
                close()
            },
            color = tint,
        )
    }
}
