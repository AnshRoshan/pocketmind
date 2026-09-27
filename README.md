# Pocketmind

An on-device, privacy-first AI assistant for Android. Pocketmind runs a small language
model (SLM) locally on your phone, can automate phone actions (open apps, send messages,
set alarms, read notifications), routes harder requests to cloud LLMs, and can be
controlled remotely through a Telegram bot.

## Architecture

Six Gradle modules (`settings.gradle.kts`), dependency direction top → bottom:

| Module | Purpose |
|---|---|
| `:app` | Compose UI (chat, models, onboarding, settings), DI wiring, foreground service, boot receiver |
| `:telegram` | Telegram bot front-end: long-polls `getUpdates`, forwards messages to the orchestrator |
| `:cloud` | `KeyVault` (encrypted API-key storage) and `CloudEngine` (OpenAI-compatible cloud LLM client: OpenRouter, Google AI, Groq, NVIDIA) |
| `:engine` | `LlamaCppEngine` — SLM inference via JNI to llama.cpp (native stub included; falls back to mock mode when the native lib is absent), `ModelDownloader` |
| `:data` | Room database (chat messages, memories), DataStore-backed `SettingsDataStore` |
| `:core` | Domain models, `Orchestrator` (input → intent → routing → action/response pipeline), `IntentClassifier`, `RoutingEngine`, `TaskPlanner`, `ActionExecutor`, `MemoryManager`, `AccessibilityBridge`, `NotificationListener` |

Request flow: user input → sanitize → memory recall → intent classification →
routing decision (local SLM / cloud / fallback) → streaming response or phone action execution.

## Tech stack

- Kotlin 1.9.22, Jetpack Compose (BOM 2024.02), Material 3
- Hilt DI, Room, DataStore, WorkManager
- Ktor client (OkHttp engine) for cloud + Telegram HTTP
- security-crypto (EncryptedSharedPreferences) for API keys
- AGP 8.3.0, Gradle 8.4, JDK 17+ (built with 21), minSdk 26, targetSdk 34
- NDK / CMake 3.22.1 for the native inference engine (arm64-v8a, x86_64)

## Building

1. Install the Android SDK (platform 34, build-tools 34.0.0, NDK, CMake 3.22.1)
   and point `local.properties` at it (`sdk.dir=...`).
2. `./gradlew assembleDebug` — produces `app/build/outputs/apk/debug/app-debug.apk`.
3. `./gradlew assembleRelease` for a minified release build.

## Native engine status

`engine/src/main/cpp/llm_jni.cpp` is a functional JNI **stub**: it compiles and loads,
but returns a mock model and mock tokens. The Kotlin layer detects the stub/mock mode
gracefully, so the whole app builds and runs. To enable real on-device inference,
vendor llama.cpp into `engine/src/main/cpp/llama.cpp` and wire the real llama.cpp API
into the JNI functions (`nativeLoadModel`, `nativeGenerate`, `nativeEmbed`, `nativeFree`).

## Setup on device

1. Onboarding: pick a model on the Models screen (downloads a GGUF from Hugging Face)
   and grant accessibility + notification-listener permissions.
2. Settings: add cloud API keys (OpenRouter / Google AI / Groq / NVIDIA) and optionally
   a Telegram bot token + chat ID, then start the foreground service to enable the
   Telegram remote control.

## Known limitations

- Voice input in the chat screen is not implemented (TODO in `ChatScreen.kt`).
- No automated tests yet.
- The `RoutingDecision.Cloud` path in `Orchestrator.kt` emits a notice; wiring
  `CloudEngine` as an alternate `SLMEngine` binding is a one-line DI change away.
