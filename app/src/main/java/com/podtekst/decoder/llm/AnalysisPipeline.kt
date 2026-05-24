package com.podtekst.decoder.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Трёхпасный анализ подтекста:
 *  pass1: факты, эмоции, таймлайн
 *  pass2: три гипотезы интерпретации
 *  pass3: ранжирование + красные флаги манипуляций + counter-script
 */
class AnalysisPipeline(private val client: RelayClient) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyze(target: String, context: List<String>): AnalysisResult {
        val ctxBlock = buildString {
            context.forEachIndexed { i, line ->
                appendLine("[$i] $line")
            }
        }.trim()

        val facts = client.chat(
            system = SYSTEM_FACTS,
            user = """
                |Контекст диалога (последние сообщения):
                |$ctxBlock
                |
                |Целевое сообщение для расшифровки:
                |"$target"
                |
                |Выдели:
                | - факты (что объективно сказано)
                | - эмоциональный тон (одно слово)
                | - чьё это сообщение по смыслу (партнёр/начальник/родитель/друг/неясно)
                | - что предшествовало в контексте
                |Ответ — короткий список из 3-5 пунктов.
            """.trimMargin(),
            temperature = 0.2,
            maxTokens = 400,
        )

        val hypotheses = client.chat(
            system = SYSTEM_HYPOTHESES,
            user = """
                |Целевое сообщение: "$target"
                |
                |Контекст:
                |$ctxBlock
                |
                |Факты пасса 1:
                |$facts
                |
                |Сформулируй РОВНО ТРИ разных интерпретации того, что человек на самом деле имеет в виду.
                |Формат:
                |1. <короткая интерпретация> — почему: <одна фраза-улика из текста>
                |2. ...
                |3. ...
                |Включи хотя бы одну "доброжелательную" версию и одну "тёмную".
            """.trimMargin(),
            temperature = 0.6,
            maxTokens = 500,
        )

        val verdict = client.chat(
            system = SYSTEM_VERDICT,
            user = """
                |Целевое сообщение: "$target"
                |Контекст: $ctxBlock
                |Факты: $facts
                |Гипотезы:
                |$hypotheses
                |
                |Сделай финальный JSON-объект СТРОГО по схеме:
                |{
                |  "subtext": "одна фраза — что на самом деле сказано",
                |  "confidence": "low" | "med" | "high",
                |  "interpretations": ["версия 1", "версия 2", "версия 3"],
                |  "red_flags": ["манипуляция 1", "манипуляция 2"],
                |  "reply_if_you_want_X": {
                |     "разрядить": "вариант ответа",
                |     "поставить_границу": "вариант ответа",
                |     "выяснить_правду": "вариант ответа"
                |  }
                |}
                |Манипуляции выбирай из словаря: гаслайт, DARVO, love-bombing, breadcrumbing,
                |stonewalling, guilt-trip, проекция, моральный шантаж, future-faking, triangulation,
                |negging, weaponized incompetence. Если ничего из этого нет — оставь массив пустым.
                |
                |Только JSON, без markdown-обёртки, без объяснений.
            """.trimMargin(),
            temperature = 0.3,
            maxTokens = 700,
        )

        val cleaned = stripJsonFences(verdict)
        return runCatching {
            json.decodeFromString(AnalysisResult.serializer(), cleaned)
        }.getOrElse {
            AnalysisResult(
                subtext = cleaned.take(280),
                confidence = "low",
                interpretations = listOf("Модель вернула невалидный JSON — выше сырой текст."),
                redFlags = emptyList(),
                replies = emptyMap(),
            )
        }
    }

    private fun stripJsonFences(raw: String): String {
        val t = raw.trim()
        if (t.startsWith("```")) {
            val firstNl = t.indexOf('\n')
            val rest = if (firstNl >= 0) t.substring(firstNl + 1) else t
            return rest.removeSuffix("```").trim()
        }
        val start = t.indexOf('{')
        val end = t.lastIndexOf('}')
        return if (start in 0..<end) t.substring(start, end + 1) else t
    }

    @Serializable
    data class AnalysisResult(
        val subtext: String,
        val confidence: String,
        val interpretations: List<String> = emptyList(),
        @SerialName("red_flags") val redFlags: List<String> = emptyList(),
        @SerialName("reply_if_you_want_X") val replies: Map<String, String> = emptyMap(),
    )

    companion object {
        private const val SYSTEM_FACTS = """
Ты беспристрастный аналитик переписки. Ты НЕ психотерапевт и НЕ принимаешь сторону.
Никаких советов на этом шаге. Только наблюдения.
"""

        private const val SYSTEM_HYPOTHESES = """
Ты опытный психолог-аналитик. Знаешь Готтмана, теорию привязанности, транзактный анализ,
паттерны манипуляций (DARVO, гаслайт, love-bombing и т.д.).
Никогда не выдавай одну версию как истину — обязательно несколько углов.
"""

        private const val SYSTEM_VERDICT = """
Ты выдаёшь ТОЛЬКО валидный JSON по запрошенной схеме. Никакого markdown, никаких комментариев.
Будь честен с уровнем уверенности: low, если данных мало; high — только при явных уликах в тексте.
"""
    }
}
