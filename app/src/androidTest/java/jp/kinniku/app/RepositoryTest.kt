package jp.kinniku.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import jp.kinniku.app.data.*
import jp.kinniku.app.data.alarm.AlarmGateway
import jp.kinniku.app.data.db.*
import jp.kinniku.app.domain.TimeProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class RepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: WorkoutRepository
    private lateinit var time: FakeTime
    private lateinit var bench: Exercise
    private class FakeTime : TimeProvider {
        var date = LocalDate.of(2026,9,6); var elapsed = 100_000L; var boot = 1
        override fun today() = date
        override fun wallMillis() = 1_800_000_000_000L + elapsed
        override fun elapsedMillis() = elapsed
        override fun bootCount() = boot
    }
    @Before fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).addCallback(AppDatabase.callback).build()
        time = FakeTime()
        repo = WorkoutRepository(db, time, SettingsStore(context), AlarmGateway(context))
        repo.settings.saveSeconds(90)
        bench = requireNotNull(db.dao().exercise(1))
    }
    @After fun cleanup() = runBlocking { repo.stopTimer(true); db.close() }
    private fun input(exercise: Exercise = bench, session: Long? = null, record: Long? = null, weight: String = "60", reps: String = "10") =
        CommitInput(time.date.toEpochDay(), session, record, exercise, weight, reps, "20", "memo")

    @Test fun seedAndUniqueActiveName() = runBlocking {
        assertEquals(53, repo.exercises.first().size)
        assertEquals(7, repo.exercises.first().count { it.exercise.bodyweight })
        try { db.dao().insertExercise(bench.copy(id = 0)); fail("unique index missing") } catch (_: android.database.sqlite.SQLiteConstraintException) { }
        repo.deleteExercise(bench.id)
        repo.saveExercise(null, bench.name, "chest", false, false)
        assertEquals(54, repo.exercises.first().size)
    }
    @Test fun incompleteInputDoesNotCreateSessionOrRecord() = runBlocking {
        try { repo.commit(input(reps = "")); fail("accepted empty reps") } catch (_: IllegalArgumentException) { }
        assertTrue(repo.day(time.date.toEpochDay()).first().isEmpty())
        assertNull(db.dao().timer())
    }
    @Test fun commitStartsTimerAndReplacesOldTimerWithElapsedRest() = runBlocking {
        val first = repo.commit(input())
        val old = requireNotNull(db.dao().timer())
        time.elapsed += 12345
        val second = repo.commit(input(session = first.sessionId, record = first.recordId))
        assertEquals(first, second)
        assertEquals(12, db.dao().set(old.setId)?.rest)
        val fresh = requireNotNull(db.dao().timer())
        repo.completeTimer(old.token, old.setId, true)
        assertEquals(fresh, db.dao().timer())
        assertEquals(1, repo.day(time.date.toEpochDay()).first().size)
    }
    @Test fun completionIsIdempotentAndClamped() = runBlocking {
        repo.commit(input()); val timer = requireNotNull(db.dao().timer()); time.elapsed += 999_000
        repo.completeTimer(timer.token, timer.setId, false); repo.completeTimer(timer.token, timer.setId, false)
        assertNull(db.dao().timer()); assertEquals(90, db.dao().set(timer.setId)?.rest)
    }
    @Test fun resetAndRebootLeaveRestEmpty() = runBlocking {
        repo.commit(input()); val timer = requireNotNull(db.dao().timer()); time.elapsed += 10_000; repo.stopTimer(true)
        assertNull(db.dao().set(timer.setId)?.rest)
        repo.commit(input()); val reboot = requireNotNull(db.dao().timer()); time.boot++; time.elapsed += 100_000
        repo.reconcileTimer(); assertNull(db.dao().timer()); assertNull(db.dao().set(reboot.setId)?.rest)
    }
    @Test fun crossMidnightRestStillSavesButContentEditFails() = runBlocking {
        repo.commit(input()); val timer = requireNotNull(db.dao().timer()); time.date = time.date.plusDays(1); time.elapsed += 100_000
        repo.reconcileTimer(); assertEquals(90, db.dao().set(timer.setId)?.rest)
        try { repo.editSet(timer.setId, "65", "8"); fail("past date edited") } catch (_: IllegalArgumentException) { }
    }
    @Test fun zeroSetParentRemainsButCalendarAndCsvExcludeIt() = runBlocking {
        val row = repo.commit(input()); val timer = requireNotNull(db.dao().timer()); repo.delete("set", timer.setId)
        assertNotNull(db.dao().record(row.recordId)); assertNull(db.dao().timer())
        assertEquals(1L, repo.exercises.first().first { it.exercise.id == bench.id }.uses)
        assertTrue(db.dao().activeDays(time.date.toEpochDay(), time.date.toEpochDay()).first().isEmpty())
        assertTrue(db.dao().csvRecords(Long.MIN_VALUE, Long.MAX_VALUE, "strength", Long.MIN_VALUE, Long.MIN_VALUE, 0, -1, 0).isEmpty())
    }
    @Test fun deletingSessionCascadesAndOrderIsCompacted() = runBlocking {
        val first = repo.commit(input()); val second = repo.commit(input(session = first.sessionId))
        repo.move("record", second.recordId, -1)
        assertEquals(second.recordId, db.dao().records(first.sessionId).first().id)
        repo.delete("record", second.recordId)
        assertEquals(0, db.dao().records(first.sessionId).single().order)
        repo.delete("session", first.sessionId)
        assertNull(db.dao().record(first.recordId)); assertNull(db.dao().timer())
    }
    @Test fun masterChangesKeepSnapshotsAndPruneTemplates() = runBlocking {
        val result = repo.commit(input())
        repo.saveTemplate(null, "胸", listOf(bench.id to 3))
        repo.saveExercise(bench.id, "新しい名前", "back", false, true)
        assertEquals("ベンチプレス", db.dao().record(result.recordId)?.name)
        assertEquals("chest", db.dao().record(result.recordId)?.bodyPart)
        assertEquals(1, repo.templates.first().single().items.size)
        repo.saveExercise(bench.id, "有酸素へ変更", "cardio", true, false)
        assertEquals(0, repo.templates.first().single().items.size)
        repo.saveTemplate(repo.templates.first().single().template.id, "空のテンプレート", emptyList())
        assertEquals("空のテンプレート", repo.templates.first().single().template.name)
        assertEquals(6000L, repo.volumes(time.date.toEpochDay(), time.date.toEpochDay()).first().single().value)
    }
    @Test fun bodyweightRecalculatesFromPastDailyAverage() = runBlocking {
        val bw = requireNotNull(db.dao().exercise(9))
        repo.commit(input(bw)); assertEquals(0L, repo.volumes(time.date.toEpochDay(), time.date.toEpochDay()).first().single().value)
        repo.saveWeight(null, time.date.minusDays(1).toEpochDay(), "60.0")
        repo.saveWeight(null, time.date.minusDays(1).toEpochDay(), "60.1")
        assertEquals(6010L, repo.volumes(time.date.toEpochDay(), time.date.toEpochDay()).first().single().value)
        repo.saveWeight(null, time.date.toEpochDay(), "70.0")
        assertEquals(7000L, repo.volumes(time.date.toEpochDay(), time.date.toEpochDay()).first().single().value)
    }
    @Test fun cardioHasNoSetsOrTimerAndPastWeightCannotEdit() = runBlocking {
        repo.saveExercise(null, "ウォーキング", "cardio", true, false)
        val exercise = repo.exercises.first().first { it.exercise.name == "ウォーキング" }.exercise
        val result = repo.commit(input(exercise))
        assertEquals(20, db.dao().record(result.recordId)?.duration); assertTrue(db.dao().sets(result.recordId).isEmpty()); assertNull(db.dao().timer())
        val past = time.date.minusDays(1).toEpochDay(); repo.saveWeight(null, past, "60")
        val weight = db.dao().weights(past, past).first().single()
        try { repo.saveWeight(weight.id, past, "61"); fail("past weight edited") } catch (_: IllegalArgumentException) { }
        repo.delete("weight", weight.id); assertTrue(db.dao().weights(past, past).first().isEmpty())
    }
    @Test fun templateOrderSurvivesCommittingLaterExerciseFirst() = runBlocking {
        val later = repo.commit(input())
        val earlier = repo.commit(input(session = later.sessionId).copy(beforeRecordId = later.recordId))
        assertEquals(listOf(earlier.recordId, later.recordId), db.dao().records(later.sessionId).map { it.id })
        assertEquals(listOf(0, 1), db.dao().records(later.sessionId).map { it.order })
    }
}
