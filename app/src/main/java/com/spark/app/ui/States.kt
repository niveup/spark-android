package com.spark.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Reusable loading / error / empty / offline states (Phase 4).

@Composable
fun LoadingState(text: String = "Loading…", modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ErrorState(
    text: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = text, color = MaterialTheme.colorScheme.error)
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyState(
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun OfflineConnectivityButton(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    description: String = "Offline. Tap to check the connection."
) {
    val tint = MaterialTheme.colorScheme.primary
    IconButton(
        onClick = onRetry,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = description
        }
    ) {
        Canvas(Modifier.size(22.dp)) {
            val scaleX = size.width / 20f
            val scaleY = size.height / 20f
            val strokeWidth = 1.8.dp.toPx()
            val waves = Path().apply {
                moveTo(1.8f * scaleX, 6.3f * scaleY)
                cubicTo(6f * scaleX, 2.1f * scaleY, 14f * scaleX, 2.1f * scaleY, 18.2f * scaleX, 6.3f * scaleY)
                moveTo(5f * scaleX, 9.8f * scaleY)
                cubicTo(8f * scaleX, 7f * scaleY, 12f * scaleX, 7f * scaleY, 15f * scaleX, 9.8f * scaleY)
                moveTo(8.2f * scaleX, 13.1f * scaleY)
                cubicTo(9.2f * scaleX, 12.2f * scaleY, 10.8f * scaleX, 12.2f * scaleY, 11.8f * scaleX, 13.1f * scaleY)
            }
            drawPath(
                path = waves,
                color = tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawCircle(
                color = tint,
                radius = strokeWidth * 0.7f,
                center = Offset(10f * scaleX, 17f * scaleY)
            )
            drawLine(
                color = tint,
                start = Offset(4f * scaleX, 4.3f * scaleY),
                end = Offset(16f * scaleX, 16.3f * scaleY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
