package jp.kinniku.app.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Query("SELECT e.*, (SELECT COUNT(*) FROM exercise_record r WHERE r.exercise_id=e.id) AS uses FROM exercise e ORDER BY uses DESC, e.created_at, e.id")
    fun exercises(): Flow<List<ExerciseUsage>>
    @Query("SELECT * FROM exercise WHERE id=:id") suspend fun exercise(id: Long): Exercise?
    @Query("SELECT COUNT(*) FROM exercise WHERE name_key=:key AND is_deleted=0 AND id!=:except") suspend fun duplicate(key: String, except: Long): Int
    @Insert suspend fun insertExercise(value: Exercise): Long
    @Update suspend fun updateExercise(value: Exercise)

    @Transaction @Query("SELECT * FROM session WHERE epoch_day=:day ORDER BY created_at,id")
    fun day(day: Long): Flow<List<SessionWithRecords>>
    @Query("SELECT * FROM session WHERE id=:id") suspend fun session(id: Long): Session?
    @Query("SELECT * FROM session WHERE epoch_day=:day ORDER BY created_at DESC,id DESC LIMIT 1") suspend fun lastSession(day: Long): Session?
    @Insert suspend fun insertSession(value: Session): Long
    @Update suspend fun updateSession(value: Session)
    @Query("DELETE FROM session WHERE id=:id") suspend fun deleteSession(id: Long)
    @Query("SELECT * FROM exercise_record WHERE id=:id") suspend fun record(id: Long): ExerciseRecord?
    @Query("SELECT * FROM exercise_record WHERE session_id=:id ORDER BY order_index,id") suspend fun records(id: Long): List<ExerciseRecord>
    @Insert suspend fun insertRecord(value: ExerciseRecord): Long
    @Update suspend fun updateRecord(value: ExerciseRecord)
    @Query("DELETE FROM exercise_record WHERE id=:id") suspend fun deleteRecord(id: Long)
    @Query("SELECT * FROM workout_set WHERE id=:id") suspend fun set(id: Long): WorkoutSet?
    @Query("SELECT * FROM workout_set WHERE exercise_record_id=:id ORDER BY order_index,id") suspend fun sets(id: Long): List<WorkoutSet>
    @Insert suspend fun insertSet(value: WorkoutSet): Long
    @Update suspend fun updateSet(value: WorkoutSet)
    @Query("UPDATE workout_set SET rest_sec=:seconds WHERE id=:id") suspend fun rest(id: Long, seconds: Int?)
    @Query("DELETE FROM workout_set WHERE id=:id") suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM rest_timer WHERE id=1") suspend fun timer(): RestTimer?
    @Query("SELECT * FROM rest_timer WHERE id=1") fun observeTimer(): Flow<RestTimer?>
    @Insert suspend fun insertTimer(value: RestTimer)
    @Query("DELETE FROM rest_timer WHERE id=1 AND token=:token AND target_set_id=:setId") suspend fun deleteTimer(token: Long, setId: Long): Int

    @Query("SELECT * FROM body_weight WHERE epoch_day BETWEEN :start AND :end ORDER BY epoch_day DESC,created_at,id")
    fun weights(start: Long, end: Long): Flow<List<BodyWeight>>
    @Query("SELECT * FROM body_weight WHERE id=:id") suspend fun weight(id: Long): BodyWeight?
    @Insert suspend fun insertWeight(value: BodyWeight): Long
    @Update suspend fun updateWeight(value: BodyWeight)
    @Query("DELETE FROM body_weight WHERE id=:id") suspend fun deleteWeight(id: Long)
    @Query("SELECT epoch_day AS day, SUM(weight_tenth_kg) AS total, COUNT(*) AS count FROM body_weight WHERE epoch_day BETWEEN :start AND :end OR epoch_day=(SELECT MAX(epoch_day) FROM body_weight WHERE epoch_day<:start) GROUP BY epoch_day ORDER BY epoch_day")
    fun averageWeights(start: Long, end: Long): Flow<List<DailyWeight>>
    @Query("SELECT s.epoch_day AS day,r.exercise_id AS exerciseId,r.snapshot_body_part AS bodyPart,r.snapshot_is_bodyweight AS bodyweight,SUM(COALESCE(w.weight_tenth_kg,0)*CAST(w.reps AS INTEGER)) AS weighted,SUM(w.reps) AS reps FROM session s JOIN exercise_record r ON r.session_id=s.id JOIN workout_set w ON w.exercise_record_id=r.id WHERE s.epoch_day BETWEEN :start AND :end AND r.snapshot_category='strength' GROUP BY s.epoch_day,r.exercise_id,r.snapshot_body_part,r.snapshot_is_bodyweight")
    fun volumes(start: Long, end: Long): Flow<List<VolumeRow>>
    @Query("SELECT DISTINCT s.epoch_day FROM session s JOIN exercise_record r ON r.session_id=s.id WHERE s.epoch_day BETWEEN :start AND :end AND (r.snapshot_category='cardio' OR EXISTS(SELECT 1 FROM workout_set w WHERE w.exercise_record_id=r.id))")
    fun activeDays(start: Long, end: Long): Flow<List<Long>>
    @Query("SELECT MIN(epoch_day) FROM session") fun earliestSession(): Flow<Long?>
    @Query("SELECT MIN(epoch_day) FROM body_weight") fun earliestWeight(): Flow<Long?>

    @Transaction @Query("SELECT * FROM template ORDER BY created_at,id") fun templates(): Flow<List<TemplateWithItems>>
    @Transaction @Query("SELECT * FROM template WHERE id=:id") suspend fun template(id: Long): TemplateWithItems?
    @Insert suspend fun insertTemplate(value: Template): Long
    @Update suspend fun updateTemplate(value: Template)
    @Query("DELETE FROM template WHERE id=:id") suspend fun deleteTemplate(id: Long)
    @Insert suspend fun insertItems(value: List<TemplateItem>)
    @Update suspend fun updateItem(value: TemplateItem)
    @Query("DELETE FROM template_item WHERE template_id=:id") suspend fun clearItems(id: Long)
    @Query("SELECT DISTINCT template_id FROM template_item WHERE exercise_id=:id") suspend fun affectedTemplates(id: Long): List<Long>
    @Query("DELETE FROM template_item WHERE exercise_id=:id") suspend fun removeExerciseFromTemplates(id: Long)

    @Query("SELECT r.*,s.epoch_day AS day,s.name AS sessionName,s.created_at AS sessionCreatedAt FROM exercise_record r JOIN session s ON s.id=r.session_id WHERE s.epoch_day BETWEEN :start AND :end AND r.snapshot_category=:category AND (r.snapshot_category='cardio' OR EXISTS(SELECT 1 FROM workout_set w WHERE w.exercise_record_id=r.id)) AND (s.epoch_day,s.created_at,s.id,r.order_index,r.id) > (:lastDay,:lastCreated,:lastSession,:lastOrder,:lastId) ORDER BY s.epoch_day,s.created_at,s.id,r.order_index,r.id LIMIT 100")
    suspend fun csvRecords(start: Long, end: Long, category: String, lastDay: Long, lastCreated: Long, lastSession: Long, lastOrder: Int, lastId: Long): List<CsvRecord>
    @Query("SELECT * FROM body_weight WHERE epoch_day BETWEEN :start AND :end AND (epoch_day,created_at,id) > (:lastDay,:lastCreated,:lastId) ORDER BY epoch_day,created_at,id LIMIT 100")
    suspend fun csvWeights(start: Long, end: Long, lastDay: Long, lastCreated: Long, lastId: Long): List<BodyWeight>
}
