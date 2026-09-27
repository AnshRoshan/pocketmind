package com.pocketmind.engine

// The engine abstractions live in :core so the orchestrator can depend on them
// without creating a module cycle. These aliases keep the historical imports working.
typealias SLMEngine = com.pocketmind.core.engine.SLMEngine
typealias ModelConfig = com.pocketmind.core.engine.ModelConfig
typealias GenerationParams = com.pocketmind.core.engine.GenerationParams
typealias ToolCallResult = com.pocketmind.core.engine.ToolCallResult
typealias ModelFormat = com.pocketmind.core.engine.ModelFormat
