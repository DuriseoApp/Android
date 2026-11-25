package com.duriseo.android.domain.appops

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRuleMatcherTest {

    private fun matches(build: BuildCondition, code: Long) =
        rule("r", build = build).matches(ANDROID_34.copy(build = code))

    @Test
    fun `FR-APP-02 AC1 EQ LTE GTE RANGE IN`() {
        assertTrue(matches(BuildCondition.Eq(10), 10))
        assertFalse(matches(BuildCondition.Eq(10), 11))

        assertTrue(matches(BuildCondition.Lte(10), 10))
        assertTrue(matches(BuildCondition.Lte(10), 3))
        assertFalse(matches(BuildCondition.Lte(10), 11))

        assertTrue(matches(BuildCondition.Gte(10), 10))
        assertTrue(matches(BuildCondition.Gte(10), 99))
        assertFalse(matches(BuildCondition.Gte(10), 9))

        assertTrue(matches(BuildCondition.Range(5, 9), 5))
        assertTrue(matches(BuildCondition.Range(5, 9), 9))
        assertFalse(matches(BuildCondition.Range(5, 9), 4))
        assertFalse(matches(BuildCondition.Range(5, 9), 10))

        assertTrue(matches(BuildCondition.In(setOf(3, 7)), 7))
        assertFalse(matches(BuildCondition.In(setOf(3, 7)), 5))
    }

    @Test
    fun `FR-APP-02 AC1 OS 버전 조건은 양 끝을 포함한다`() {
        val between = rule("r", os = OsCondition(min = 30, max = 34))
        assertTrue(between.matches(ANDROID_34.copy(osVersion = 30)))
        assertTrue(between.matches(ANDROID_34.copy(osVersion = 34)))
        assertFalse(between.matches(ANDROID_34.copy(osVersion = 29)))
        assertFalse(between.matches(ANDROID_34.copy(osVersion = 35)))

        // 한쪽이 null이면 그쪽은 통과, osVersion이 null이면 조건 없음
        assertTrue(rule("r", os = OsCondition(min = null, max = 34)).matches(ANDROID_34.copy(osVersion = 1)))
        assertTrue(rule("r", os = OsCondition(min = 30, max = null)).matches(ANDROID_34.copy(osVersion = 99)))
        assertTrue(rule("r", os = null).matches(ANDROID_34))
    }

    @Test
    fun `FR-APP-02 AC1 다른 플랫폼은 맞지 않는다`() {
        assertFalse(rule("r", platform = AppPlatform.IOS).matches(ANDROID_34))
        assertEquals(true, rule("r", platform = AppPlatform.ANDROID).matches(ANDROID_34))
    }
}
