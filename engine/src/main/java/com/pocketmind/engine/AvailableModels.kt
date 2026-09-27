package com.pocketmind.engine

import com.pocketmind.core.model.ModelInfo

object AvailableModels {
    val catalog: List<ModelInfo> = listOf(
        ModelInfo(
            id = "qwen2.5-0.5b",
            name = "Qwen2.5-0.5B",
            description = "Tiny & fast. Quick responses, basic tasks. Best for older devices.",
            sizeBytes = 500_000_000L,
            format = "GGUF",
            quantization = "Q4_K_M",
            contextLength = 2048,
            supportsToolCalling = false,
            supportsVision = false,
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf"
        ),
        ModelInfo(
            id = "smollm-1.7b",
            name = "SmolLM-1.7B",
            description = "Balanced. Good for most everyday tasks. Recommended for mid-range phones.",
            sizeBytes = 1_000_000_000L,
            format = "GGUF",
            quantization = "Q4_K_M",
            contextLength = 2048,
            supportsToolCalling = false,
            supportsVision = false,
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-1.7B-Instruct-GGUF/resolve/main/smollm2-1.7b-instruct-q4_k_m.gguf"
        ),
        ModelInfo(
            id = "phi-3-mini",
            name = "Phi-3-mini 3.8B",
            description = "Powerful reasoning. Best quality for flagship phones (8GB+ RAM).",
            sizeBytes = 2_200_000_000L,
            format = "GGUF",
            quantization = "Q4_K_M",
            contextLength = 4096,
            supportsToolCalling = true,
            supportsVision = false,
            downloadUrl = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-gguf/resolve/main/Phi-3-mini-4k-instruct-q4.gguf"
        ),
        ModelInfo(
            id = "gemma-3n-e2b",
            name = "Gemma 3n E2B",
            description = "Google's on-device model. Supports vision (images). Great all-rounder.",
            sizeBytes = 1_500_000_000L,
            format = "LITERT",
            quantization = "INT4",
            contextLength = 4096,
            supportsToolCalling = true,
            supportsVision = true,
            downloadUrl = "https://huggingface.co/google/gemma-3n-E2B-it-litert-preview/resolve/main/gemma-3n-E2B-it-int4.task"
        ),
        ModelInfo(
            id = "phonelm-1.5b",
            name = "PhoneLM-1.5B",
            description = "Designed for Android. Specializes in phone control and Android intents.",
            sizeBytes = 900_000_000L,
            format = "GGUF",
            quantization = "Q4_K_M",
            contextLength = 2048,
            supportsToolCalling = true,
            supportsVision = false,
            downloadUrl = "https://huggingface.co/mllmTeam/PhoneLM-1.5B-Instruct-GGUF/resolve/main/phonelm-1.5b-instruct-q4_k_m.gguf"
        )
    )
}
