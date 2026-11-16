package com.spark.app.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.ui.theme.SparkSpacing

@Composable
fun LoginScreen(
    state: AuthUiState,
    onLogin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep credentials in memory only; do not put the passcode in saved UI state.
    var passcode by remember { mutableStateOf("") }
    var errorEdited by remember(state.message) { mutableStateOf(false) }
    val busy = state.status == AuthStatus.LOGGING_IN
    val hasError = state.message != null && !busy && !errorEdited
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = SparkSpacing.pageGutter, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "SPARK",
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.5.sp
            ),
            color = colors.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A FINITE READING FEED",
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant,
            letterSpacing = 1.4.sp
        )
        Spacer(Modifier.height(28.dp))
        Text(
            text = "Make room for a good idea.",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Serif,
                lineHeight = 32.sp
            ),
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 400.dp)
                .semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Your personal deck of short, useful ideas.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp),
            shape = RoundedCornerShape(24.dp),
            color = colors.surfaceContainerLow,
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Unlock your feed",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onSurface
                )
                Text(
                    text = "Enter your private passcode to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                OutlinedTextField(
                    value = passcode,
                    onValueChange = {
                        passcode = it
                        errorEdited = true
                    },
                    label = { Text("Passcode") },
                    singleLine = true,
                    enabled = !busy,
                    isError = hasError,
                    supportingText = {
                        if (hasError) {
                            Text(state.message.orEmpty())
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = { if (!busy && passcode.isNotBlank()) onLogin(passcode) }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onLogin(passcode) },
                    enabled = !busy && passcode.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (busy) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .height(18.dp)
                                .width(18.dp)
                        )
                    }
                    Text(if (busy) "Unlocking…" else "Unlock")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "Stays logged in on this phone.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
