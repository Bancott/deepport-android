package com.example.deepseekclone.data

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 公开 API 没有官方的 `latest_reminder` 角色，时间锚点放在 system 提示里，
 * 这里固定住格式：<日期>,<星期>,<地点>,<平台>,<语言>。
 */
class SystemPromptTest {

    @Test
    fun reminderLineMatchesOfficialFormat() {
        val line = DeepSeekApi.reminderLine()
        val pattern = Regex("""\d{4}-\d{2}-\d{2},星期[一二三四五六日],中国,Android,[A-Za-z-]+""")
        assertTrue("unexpected reminder: $line", pattern.matches(line))
    }

    @Test
    fun systemPromptCarriesDateAndIdentity() {
        val prompt = DeepSeekApi.systemPrompt()
        assertTrue(prompt.contains(DeepSeekApi.SYSTEM_PROMPT))
        assertTrue(prompt.contains("当前时间："))
        assertTrue(prompt.contains(DeepSeekApi.reminderLine()))
    }
}
