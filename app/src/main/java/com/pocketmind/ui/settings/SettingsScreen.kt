package com.pocketmind.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::saveSettings) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cloud API Keys Section
            SectionCard(
                icon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary) },
                title = "Cloud API Keys",
                subtitle = "Optional — for complex tasks that need bigger models"
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable cloud fallback", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = uiState.cloudEnabled,
                        onCheckedChange = viewModel::onCloudEnabledChanged
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                ApiKeyField(
                    label = "OpenRouter API Key",
                    value = uiState.openRouterKey,
                    onValueChange = viewModel::onOpenRouterKeyChanged,
                    placeholder = "sk-or-..."
                )
                Spacer(modifier = Modifier.height(8.dp))
                ApiKeyField(
                    label = "Google AI (Gemini) Key",
                    value = uiState.googleAiKey,
                    onValueChange = viewModel::onGoogleAiKeyChanged,
                    placeholder = "AIza..."
                )
                Spacer(modifier = Modifier.height(8.dp))
                ApiKeyField(
                    label = "Groq API Key",
                    value = uiState.groqKey,
                    onValueChange = viewModel::onGroqKeyChanged,
                    placeholder = "gsk_..."
                )
                Spacer(modifier = Modifier.height(8.dp))
                ApiKeyField(
                    label = "NVIDIA API Key",
                    value = uiState.nvidiaKey,
                    onValueChange = viewModel::onNvidiaKeyChanged,
                    placeholder = "nvapi-..."
                )
            }

            HorizontalDivider()

            // Telegram Section
            SectionCard(
                icon = { Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.primary) },
                title = "Telegram Remote Control",
                subtitle = "Control Pocketmind from anywhere via your own Telegram bot"
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                ApiKeyField(
                    label = "Bot Token",
                    value = uiState.telegramBotToken,
                    onValueChange = viewModel::onTelegramTokenChanged,
                    placeholder = "123456:ABC-DEF..."
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.telegramChatId,
                    onValueChange = viewModel::onTelegramChatIdChanged,
                    label = { Text("Your Chat ID") },
                    placeholder = { Text("123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            HorizontalDivider()

            // Cloud Section Info
            SectionCard(
                icon = { Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.primary) },
                title = "About Cloud Escalation",
                subtitle = null
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "When on-device AI isn't enough for complex tasks, Pocketmind can " +
                            "route requests to cloud models using your own API keys.\n\n" +
                            "• Your keys are stored encrypted on-device\n" +
                            "• Cloud is only used with your explicit consent\n" +
                            "• You can disable cloud entirely above",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = viewModel::saveSettings,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                Text(if (uiState.isSaving) "Saving..." else "Save Settings")
            }
        }
    }
}

@Composable
private fun SectionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon()
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (subtitle != null) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    var showKey by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { showKey = !showKey }) {
                Text(if (showKey) "🙈" else "👁", style = MaterialTheme.typography.labelLarge)
            }
        },
        singleLine = true
    )
}
