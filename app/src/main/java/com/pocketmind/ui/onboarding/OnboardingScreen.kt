package com.pocketmind.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pocketmind.ui.settings.SettingsViewModel
import kotlinx.coroutines.launch

private data class ModelOption(
    val id: String,
    val name: String,
    val description: String,
    val size: String,
    val emoji: String
)

private val modelOptions = listOf(
    ModelOption("qwen2.5-0.5b", "Tiny & Fast", "Quick responses, basic tasks", "~500MB", "⚡"),
    ModelOption("smollm-1.7b", "Balanced", "Good for most everyday tasks", "~1GB", "🎯"),
    ModelOption("gemma-3n-e2b", "Powerful", "Better reasoning + Vision support", "~1.5GB", "🚀"),
    ModelOption("phi-3-mini", "Beast Mode", "Best quality, for flagship phones", "~2.2GB", "🦁")
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var selectedModelId by remember { mutableStateOf(modelOptions[1].id) }
    var telegramToken by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val totalSteps = 4

    Column(modifier = Modifier.fillMaxSize()) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (currentStep + 1).toFloat() / totalSteps },
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "onboarding"
        ) { step ->
            when (step) {
                0 -> WelcomeStep(
                    onNext = { currentStep++ }
                )
                1 -> ModelSelectionStep(
                    selectedModelId = selectedModelId,
                    onModelSelected = { selectedModelId = it },
                    onNext = { currentStep++ },
                    onBack = { currentStep-- }
                )
                2 -> PermissionsStep(
                    onNext = { currentStep++ },
                    onBack = { currentStep-- }
                )
                3 -> TelegramStep(
                    token = telegramToken,
                    onTokenChanged = { telegramToken = it },
                    onFinish = {
                        scope.launch {
                            settingsViewModel.setOnboardingComplete()
                            onFinish()
                        }
                    },
                    onBack = { currentStep-- }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧠", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Pocketmind",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your phone is the server.\nYour AI never sleeps.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        val features = listOf(
            Triple(Icons.Default.Memory, "On-Device AI", "Runs locally, no data leaves your phone"),
            Triple(Icons.Default.PhoneAndroid, "Phone Control", "Open apps, send messages, set alarms"),
            Triple(Icons.Default.Security, "Total Privacy", "Your conversations stay on your phone"),
            Triple(Icons.Default.Bolt, "Works Offline", "Full functionality without internet")
        )

        features.forEach { (icon, title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Get Started")
        }
    }
}

@Composable
private fun ModelSelectionStep(
    selectedModelId: String,
    onModelSelected: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Choose Your AI Model", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "This model will run directly on your phone. You can change it later.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(modelOptions) { option ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = option.id == selectedModelId, onClick = { onModelSelected(option.id) }),
                    colors = CardDefaults.cardColors(
                        containerColor = if (option.id == selectedModelId)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    ),
                    border = if (option.id == selectedModelId) {
                        androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    } else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = option.id == selectedModelId, onClick = { onModelSelected(option.id) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(option.emoji, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(option.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(option.description, style = MaterialTheme.typography.bodySmall)
                            Text(option.size, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(2f)) { Text("Download & Continue") }
        }
    }
}

@Composable
private fun PermissionsStep(onNext: () -> Unit, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("Grant Permissions", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Pocketmind needs these to control your phone on your behalf.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        val permissions = listOf(
            "♿ Accessibility Service" to "Required for UI automation (opening apps, tapping, typing)",
            "🔔 Notification Access" to "Read notifications to summarize what you missed",
            "👥 Contacts" to "Look up contact names when sending messages",
            "📅 Calendar" to "Create events and remind you of upcoming meetings",
            "📍 Location" to "Used locally for weather and directions — never sent to cloud",
            "🎤 Microphone" to "Optional voice input"
        )

        permissions.forEach { (title, desc) ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onNext, modifier = Modifier.weight(2f)) { Text("Grant Permissions") }
        }
    }
}

@Composable
private fun TelegramStep(
    token: String,
    onTokenChanged: (String) -> Unit,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Telegram Remote Control", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Optional — send commands to Pocketmind from anywhere via Telegram.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("How to set up:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Open Telegram and message @BotFather", style = MaterialTheme.typography.bodySmall)
                Text("2. Send /newbot and follow instructions", style = MaterialTheme.typography.bodySmall)
                Text("3. Copy the bot token and paste below", style = MaterialTheme.typography.bodySmall)
                Text("4. Start a chat with your new bot to get your chat ID", style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChanged,
            label = { Text("Bot Token (optional)") },
            placeholder = { Text("123456:ABC-DEF1234ghIkl-zyx...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
            Button(onClick = onFinish, modifier = Modifier.weight(2f)) { Text("Start Using Pocketmind 🚀") }
        }
        TextButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
