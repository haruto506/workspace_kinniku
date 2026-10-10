package jp.kinniku.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

enum class BodyPart(val code: String, val label: String) {
    CHEST("chest", "胸"), BACK("back", "背中"), LEGS("legs", "脚"),
    SHOULDERS("shoulders", "肩"), ARMS("arms", "腕"), CORE("core", "体幹"), CARDIO("cardio", "有酸素");
    companion object { fun from(code: String) = entries.first { it.code == code } }
}

interface TimeProvider {
    fun today(): LocalDate
    fun wallMillis(): Long
    fun elapsedMillis(): Long
    fun bootCount(): Int
}

object Rules {
    fun name(value: String, required: Boolean = true): String {
        val clean = value.trim { it.isWhitespace() || Character.isSpaceChar(it) }
        require(!required || clean.isNotEmpty()) { "名前を入力してください" }
        require(clean.codePointCount(0, clean.length) <= 50) { "名前は50文字以内で入力してください" }
        return clean
    }
    fun nameKey(value: String): String = Normalizer.normalize(name(value), Normalizer.Form.NFKC)
        .replace(Regex("[\\s\\p{Z}]+"), " ").lowercase(Locale.ROOT)
    fun memo(value: String): String? {
        require(value.codePointCount(0, value.length) <= 500) { "メモは500文字以内で入力してください" }
        return value.ifEmpty { null }
    }
    private fun normalized(value: String) = Normalizer.normalize(value.trim(), Normalizer.Form.NFKC)
    fun integer(value: String, max: Int = 999): Int {
        val text = normalized(value)
        require(text.matches(Regex("[0-9]+"))) { "整数を入力してください" }
        return (text.toIntOrNull() ?: 0).also { require(it in 1..max) { "1〜${max}で入力してください" } }
    }
    fun tenth(value: String, body: Boolean = false): Int {
        val text = normalized(value)
        require(text.matches(Regex("[0-9]+(?:\\.[0-9]+)?"))) { "数値を入力してください" }
        val amount = try { BigDecimal(text).movePointRight(1).intValueExact() } catch (_: ArithmeticException) { 0 }
        require(if (body) amount in 1..9999 else amount in 5..9995 && amount % 5 == 0) {
            if (body) "体重は0.1〜999.9 kg、0.1 kg刻みです" else "重量は0.5〜999.5 kg、0.5 kg刻みです"
        }
        return amount
    }
    fun editable(day: Long, today: LocalDate) { require(day == today.toEpochDay()) { "日付が変わりました。当日の記録のみ編集できます" } }
    fun notFuture(day: Long, today: LocalDate) { require(day <= today.toEpochDay()) { "未来の日付は選択できません" } }
    fun average(weights: List<Int>): Int = if (weights.isEmpty()) 0 else
        BigDecimal(weights.sumOf { it.toLong() }).divide(BigDecimal(weights.size), 0, RoundingMode.HALF_UP).intValueExact()
    fun resolveWeight(day: Long, weights: Map<Long, Int>): Int = weights.keys.filter { it <= day }.maxOrNull()?.let { weights[it] } ?: 0
    fun volume(weight: Int, reps: Int): Long = weight.toLong() * reps
    fun rest(start: Long, now: Long, target: Int): Int = ((now - start).coerceAtLeast(0) / 1000).coerceAtMost(target.toLong()).toInt()
    fun rebooted(start: Long, now: Long, oldBoot: Int, boot: Int) = oldBoot != boot || now < start
    fun graphStart(today: LocalDate, period: Int): LocalDate = when (period) {
        0 -> today.minusMonths(1).plusDays(1)
        1 -> today.minusMonths(3).plusDays(1)
        2 -> today.minusYears(1).plusDays(1)
        else -> LocalDate.MIN
    }
    fun weekStart(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value % 7).toLong())
    fun kg(tenth: Long, fixed: Boolean = false): String = BigDecimal.valueOf(tenth, 1).let {
        if (fixed) it.setScale(1).toPlainString() else it.stripTrailingZeros().toPlainString()
    }
    fun clock(seconds: Int) = String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
    fun csv(values: List<String>): String = values.joinToString(",") { value ->
        if (value.any { it == ',' || it == '"' || it == '\r' || it == '\n' }) "\"${value.replace("\"", "\"\"")}\"" else value
    } + "\r\n"
}
