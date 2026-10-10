package jp.kinniku.app.data.csv

import android.content.*
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import jp.kinniku.app.data.db.AppDatabase
import jp.kinniku.app.domain.BodyPart
import jp.kinniku.app.domain.Rules
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Singleton
class CsvExporter @Inject constructor(@ApplicationContext private val context: Context, private val db: AppDatabase) {
    private val lock = Mutex()
    private val resolver get() = context.contentResolver
    private val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val dao = db.dao()
    suspend fun reap() = withContext(Dispatchers.IO) { lock.withLock { reapInternal() } }
    private fun reapInternal() {
        runCatching {
            val selection = "${MediaStore.MediaColumns.IS_PENDING}=1 AND ${MediaStore.MediaColumns.OWNER_PACKAGE_NAME}=? AND ${MediaStore.MediaColumns.DATE_MODIFIED}<? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
            resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME), selection,
                arrayOf(context.packageName, (System.currentTimeMillis() / 1000 - 86400).toString(), "${Environment.DIRECTORY_DOWNLOADS}/"), null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (Regex("(strength|cardio|weight)_[0-9]{8}_[0-9]{6}(_[0-9]+)?\\.csv").matches(cursor.getString(1)))
                        resolver.delete(ContentUris.withAppendedId(collection, cursor.getLong(0)), null, null)
                }
            }
        }
    }
    private fun uniqueName(base: String): String {
        var candidate = "$base.csv"; var index = 2
        while (resolver.query(collection, arrayOf(MediaStore.MediaColumns._ID), "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(candidate, "${Environment.DIRECTORY_DOWNLOADS}/"), null)?.use { it.count > 0 } == true) {
            candidate = "${base}_${index++}.csv"
        }
        return candidate
    }
    suspend fun export(start: Long, end: Long, progress: (String) -> Unit): List<String> = withContext(Dispatchers.IO) {
        require(start <= end) { "開始日は終了日以前にしてください" }
        lock.withLock {
            reapInternal()
            val results = mutableListOf<String>()
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            for (kind in listOf("strength", "cardio", "weight")) {
                currentCoroutineContext().ensureActive()
                val name = uniqueName("${kind}_$stamp")
                var pending: Uri? = null
                try {
                    progress("$name を書き出しています")
                    val uri = requireNotNull(resolver.insert(collection, ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    })) { "保存先を作成できませんでした" }
                    pending = uri
                    OutputStreamWriter(requireNotNull(resolver.openOutputStream(uri)), Charsets.UTF_8).buffered().use { writer ->
                        writer.write("\uFEFF")
                        writer.write(Rules.csv(when (kind) {
                            "strength" -> listOf("日付", "セッション名", "種目", "部位", "セット数", "セット詳細")
                            "cardio" -> listOf("日付", "セッション名", "種目", "実施時間")
                            else -> listOf("日付", "体重")
                        }))
                        var lastDay = Long.MIN_VALUE; var lastCreated = Long.MIN_VALUE; var lastId = 0L
                        var lastSession = 0L; var lastOrder = -1
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            if (kind == "weight") {
                                val page = dao.csvWeights(start, end, lastDay, lastCreated, lastId)
                                if (page.isEmpty()) break
                                page.forEach { row -> writer.write(Rules.csv(listOf(LocalDate.ofEpochDay(row.day).toString(), Rules.kg(row.weight.toLong(), true)))) }
                                page.last().let { lastDay = it.day; lastCreated = it.createdAt; lastId = it.id }
                            } else {
                                // Only the small read page owns a transaction; disk writes never hold it.
                                val page = db.withTransaction {
                                    dao.csvRecords(start, end, kind, lastDay, lastCreated, lastSession, lastOrder, lastId).map { it to if (kind == "strength") dao.sets(it.record.id) else emptyList() }
                                }
                                if (page.isEmpty()) break
                                page.forEach { (row, sets) ->
                                    val common = listOf(LocalDate.ofEpochDay(row.day).toString(), row.sessionName.orEmpty(), row.record.name)
                                    val values = if (kind == "strength") common + listOf(BodyPart.from(row.record.bodyPart).label, sets.size.toString(),
                                        sets.joinToString("; ") { "${if (row.record.bodyweight) "" else Rules.kg(requireNotNull(it.weight).toLong())}x${it.reps}" })
                                    else common + requireNotNull(row.record.duration).toString()
                                    writer.write(Rules.csv(values))
                                }
                                page.last().first.let { lastDay = it.day; lastCreated = it.sessionCreatedAt; lastSession = it.record.sessionId; lastOrder = it.record.order; lastId = it.record.id }
                            }
                        }
                    }
                    currentCoroutineContext().ensureActive()
                    check(resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null) == 1)
                    pending = null
                    results += "保存済み: $name"
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { results += "失敗: $name（空き容量と保存先を確認してください）" }
                finally { pending?.let { runCatching { resolver.delete(it, null, null) } } }
            }
            results
        }
    }
}
