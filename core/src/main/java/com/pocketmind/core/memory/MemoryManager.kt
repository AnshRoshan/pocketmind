package com.pocketmind.core.memory

import com.pocketmind.core.model.MemoryType
import javax.inject.Inject
import javax.inject.Singleton

data class Memory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val content: String,
    val type: MemoryType = MemoryType.FACT,
    val importance: Float = 0.5f,
    val timestamp: Long = System.currentTimeMillis()
)

@Singleton
class MemoryManager @Inject constructor() {

    // In-memory store for MVP; backed by Room DB in full version
    private val memories = mutableListOf<Memory>()

    fun recall(query: String, topK: Int = 5): List<Memory> {
        val queryWords = query.lowercase().split(" ").filter { it.length > 3 }
        return memories
            .map { mem -> Pair(mem, queryWords.count { mem.content.lowercase().contains(it) }) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(topK)
            .map { it.first }
    }

    fun remember(content: String, type: MemoryType = MemoryType.FACT) {
        if (content.isBlank()) return
        // Avoid duplicates
        if (memories.any { it.content.equals(content, ignoreCase = true) }) return
        memories.add(0, Memory(content = content, type = type))
        // Keep last 200 memories
        if (memories.size > 200) memories.removeAt(memories.size - 1)
    }

    fun forget(memoryId: String) {
        memories.removeIf { it.id == memoryId }
    }

    fun getAllMemories(): List<Memory> = memories.toList()

    fun clear() = memories.clear()
}
