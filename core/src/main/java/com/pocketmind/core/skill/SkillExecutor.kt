package com.pocketmind.core.skill

import javax.inject.Inject
import javax.inject.Singleton

data class Skill(
    val name: String,
    val description: String,
    val trigger: String,   // "manual", "cron:0 7 * * *", "keyword:morning"
    val steps: List<SkillStep>
)

data class SkillStep(
    val action: String,
    val params: Map<String, String> = emptyMap()
)

@Singleton
class SkillExecutor @Inject constructor() {

    private val installedSkills = mutableListOf<Skill>()

    fun installSkill(skill: Skill) {
        installedSkills.removeIf { it.name == skill.name }
        installedSkills.add(skill)
    }

    fun getSkills(): List<Skill> = installedSkills.toList()

    fun findSkillByKeyword(input: String): Skill? {
        return installedSkills.firstOrNull { skill ->
            if (skill.trigger.startsWith("keyword:")) {
                val keyword = skill.trigger.removePrefix("keyword:")
                input.lowercase().contains(keyword.lowercase())
            } else false
        }
    }

    suspend fun execute(skill: Skill, params: Map<String, String> = emptyMap()): String {
        val results = StringBuilder()
        for (step in skill.steps) {
            results.appendLine("Step: ${step.action} — ${step.params}")
        }
        return results.toString()
    }

    // Built-in skills
    fun loadDefaultSkills() {
        installedSkills.add(Skill(
            name = "morning_briefing",
            description = "Get weather, calendar, and news each morning",
            trigger = "keyword:morning briefing",
            steps = listOf(
                SkillStep("read_notifications"),
                SkillStep("search_web", mapOf("query" to "top news today")),
                SkillStep("compose_message", mapOf("template" to "Good morning! Here's your briefing:\n{{notifications}}\n{{news}}"))
            )
        ))
    }
}
