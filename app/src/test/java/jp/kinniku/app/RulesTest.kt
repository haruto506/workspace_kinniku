package jp.kinniku.app

import jp.kinniku.app.domain.Rules
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    @Test fun weightUsesExactHalfKgSteps() {
        assertEquals(605, Rules.tenth("６０．５")); assertEquals(9995, Rules.tenth("999.5"))
        listOf("0", "60.1", "1000", "1e2", "NaN", "-1", "0.05", "1.0001").forEach { assertThrows(IllegalArgumentException::class.java) { Rules.tenth(it) } }
    }
    @Test fun bodyWeightAcceptsTenthPrecision() {
        assertEquals(1, Rules.tenth("0.1", true)); assertEquals(9999, Rules.tenth("999.9", true))
        assertThrows(IllegalArgumentException::class.java) { Rules.tenth("60.01", true) }
    }
    @Test fun integersAndLimits() {
        assertEquals(999, Rules.integer("９９９")); assertEquals(3600, Rules.integer("3600", 3600))
        listOf("", "0", "1.0", "-1", "1000", "99999999999999999999").forEach { assertThrows(IllegalArgumentException::class.java) { Rules.integer(it) } }
        assertThrows(IllegalArgumentException::class.java) { Rules.integer("21", 20) }
    }
    @Test fun namesAreNormalizedButDisplayIsPreserved() {
        assertEquals("bench press", Rules.nameKey("　ＢＥＮＣＨ　　Press  "))
        assertEquals("ＢＥＮＣＨ", Rules.name("　ＢＥＮＣＨ　"))
        assertEquals("", Rules.name("　 ", false))
        assertThrows(IllegalArgumentException::class.java) { Rules.name("　") }
    }
    @Test fun lengthIsUnicodeCodePoints() {
        assertEquals(100, Rules.name("💪".repeat(50)).length)
        assertThrows(IllegalArgumentException::class.java) { Rules.name("💪".repeat(51)) }
        assertEquals("  \n", Rules.memo("  \n"))
        assertThrows(IllegalArgumentException::class.java) { Rules.memo("a".repeat(501)) }
    }
    @Test fun averagesRoundHalfUpBeforeVolume() {
        assertEquals(601, Rules.average(listOf(600, 601)))
        assertEquals(6010L, Rules.volume(Rules.average(listOf(600, 601)), 10))
    }
    @Test fun bodyweightUsesLatestPastDailyAverage() {
        val weights = mapOf(10L to 600, 15L to 620, 30L to 900)
        assertEquals(620, Rules.resolveWeight(15, weights)); assertEquals(620, Rules.resolveWeight(20, weights)); assertEquals(0, Rules.resolveWeight(9, weights))
    }
    @Test fun volumeDoesNotOverflowIntAtFiveYearScale() {
        assertEquals(1_497_750_750_000L, Rules.volume(9995, 999) * 150_000)
        assertTrue(Rules.volume(9995, 999) * 150_000 > Int.MAX_VALUE)
    }
    @Test fun rollingMonthIncludesBothBounds() {
        assertEquals(LocalDate.of(2025,3,1), Rules.graphStart(LocalDate.of(2025,3,31), 0))
        assertEquals(LocalDate.of(2024,3,1), Rules.graphStart(LocalDate.of(2024,3,31), 0))
        assertEquals(LocalDate.of(2024,2,29), Rules.graphStart(LocalDate.of(2025,2,28), 2))
    }
    @Test fun weeksStartOnSunday() {
        assertEquals(LocalDate.of(2026,9,6), Rules.weekStart(LocalDate.of(2026,9,12)))
        assertEquals(LocalDate.of(2026,9,6), Rules.weekStart(LocalDate.of(2026,9,6)))
    }
    @Test fun editPermissionIsCheckedAtCommitDate() {
        val today = LocalDate.of(2026,9,6)
        Rules.editable(today.toEpochDay(), today)
        assertThrows(IllegalArgumentException::class.java) { Rules.editable(today.minusDays(1).toEpochDay(), today) }
        assertThrows(IllegalArgumentException::class.java) { Rules.notFuture(today.plusDays(1).toEpochDay(), today) }
    }
    @Test fun timerUsesMonotonicElapsedAndClampsAllPaths() {
        assertEquals(0, Rules.rest(1000, 1999, 90)); assertEquals(1, Rules.rest(1000, 2000, 90))
        assertEquals(90, Rules.rest(1000, 900000, 90)); assertEquals(0, Rules.rest(1000, 999, 90))
    }
    @Test fun rebootDetectedEvenWhenNewUptimeExceedsOldStart() {
        assertTrue(Rules.rebooted(1_800_000, 7_200_000, 2, 3))
        assertFalse(Rules.rebooted(1_800_000, 7_200_000, 2, 2))
        assertTrue(Rules.rebooted(1_800_000, 1000, 2, 2))
    }
    @Test fun csvEscapesSpecialCharactersAndUsesCrLf() {
        assertEquals("2026-09-06,\"朝,胸\",\"a\"\"b\",\"c\nd\"\r\n", Rules.csv(listOf("2026-09-06", "朝,胸", "a\"b", "c\nd")))
        assertEquals("日付,体重\r\n", Rules.csv(listOf("日付", "体重")))
    }
    @Test fun outputFormatting() {
        assertEquals("60", Rules.kg(600)); assertEquals("60.5", Rules.kg(605)); assertEquals("60.0", Rules.kg(600, true))
        assertEquals("01:30", Rules.clock(90)); assertEquals("60:00", Rules.clock(3600))
    }
}
