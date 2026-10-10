package jp.kinniku.app.data.db

import androidx.room.*

@Entity(tableName = "exercise", indices = [Index("is_deleted")])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, @ColumnInfo(name = "name_key") val nameKey: String,
    @ColumnInfo(name = "body_part") val bodyPart: String,
    val category: String = "strength", @ColumnInfo(name = "is_bodyweight") val bodyweight: Boolean = false,
    @ColumnInfo(name = "is_deleted") val deleted: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = createdAt
)
@Entity(tableName = "session", indices = [Index("epoch_day")])
data class Session(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "epoch_day") val day: Long, val name: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long)

@Entity(tableName = "exercise_record", indices = [Index("session_id"), Index("exercise_id")], foreignKeys = [
    ForeignKey(entity = Session::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exercise_id"])])
data class ExerciseRecord(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "order_index") val order: Int,
    @ColumnInfo(name = "snapshot_name") val name: String,
    @ColumnInfo(name = "snapshot_body_part") val bodyPart: String,
    @ColumnInfo(name = "snapshot_category") val category: String,
    @ColumnInfo(name = "snapshot_is_bodyweight") val bodyweight: Boolean,
    @ColumnInfo(name = "duration_min") val duration: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long)

@Entity(tableName = "workout_set", indices = [Index("exercise_record_id")], foreignKeys = [
    ForeignKey(entity = ExerciseRecord::class, parentColumns = ["id"], childColumns = ["exercise_record_id"], onDelete = ForeignKey.CASCADE)])
data class WorkoutSet(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "exercise_record_id") val recordId: Long,
    @ColumnInfo(name = "order_index") val order: Int,
    @ColumnInfo(name = "weight_tenth_kg") val weight: Int?, val reps: Int,
    @ColumnInfo(name = "rest_sec") val rest: Int? = null, val memo: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long)

@Entity(tableName = "body_weight", indices = [Index("epoch_day")])
data class BodyWeight(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "epoch_day") val day: Long,
    @ColumnInfo(name = "weight_tenth_kg") val weight: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long)

@Entity(tableName = "template")
data class Template(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long)

@Entity(tableName = "template_item", indices = [Index("template_id"), Index("exercise_id")], foreignKeys = [
    ForeignKey(entity = Template::class, parentColumns = ["id"], childColumns = ["template_id"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exercise_id"])])
data class TemplateItem(@PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "template_id") val templateId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "order_index") val order: Int,
    @ColumnInfo(name = "set_count") val setCount: Int?)

@Entity(tableName = "rest_timer", indices = [Index("target_set_id")], foreignKeys = [
    ForeignKey(entity = WorkoutSet::class, parentColumns = ["id"], childColumns = ["target_set_id"], onDelete = ForeignKey.CASCADE)])
data class RestTimer(@PrimaryKey val id: Int = 1, val token: Long,
    @ColumnInfo(name = "target_set_id") val setId: Long,
    @ColumnInfo(name = "started_elapsed_millis") val startedElapsed: Long,
    @ColumnInfo(name = "started_boot_count") val boot: Int,
    @ColumnInfo(name = "started_at_millis") val startedAt: Long,
    @ColumnInfo(name = "target_sec") val target: Int)

data class RecordWithSets(@Embedded val record: ExerciseRecord,
    @Relation(parentColumn = "id", entityColumn = "exercise_record_id") val sets: List<WorkoutSet>) {
    val sortedSets get() = sets.sortedWith(compareBy({ it.order }, { it.id }))
}
data class SessionWithRecords(@Embedded val session: Session,
    @Relation(entity = ExerciseRecord::class, parentColumn = "id", entityColumn = "session_id") val records: List<RecordWithSets>) {
    val sortedRecords get() = records.sortedWith(compareBy({ it.record.order }, { it.record.id }))
}
data class TemplateWithItems(@Embedded val template: Template,
    @Relation(parentColumn = "id", entityColumn = "template_id") val items: List<TemplateItem>)
data class ExerciseUsage(@Embedded val exercise: Exercise, val uses: Long)
data class VolumeRow(val day: Long, val exerciseId: Long, val bodyPart: String, val bodyweight: Boolean, val weighted: Long, val reps: Long)
data class DailyWeight(val day: Long, val total: Long, val count: Int)
data class CsvRecord(@Embedded val record: ExerciseRecord, val day: Long, val sessionName: String?, val sessionCreatedAt: Long)
