package jp.kinniku.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import jp.kinniku.app.domain.Rules

@Database(entities = [Exercise::class, Session::class, ExerciseRecord::class, WorkoutSet::class,
    BodyWeight::class, Template::class, TemplateItem::class, RestTimer::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): WorkoutDao
    companion object {
        val callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE UNIQUE INDEX idx_exercise_name_key_active ON exercise(name_key) WHERE is_deleted=0")
                InitialExercises.values.forEach { (name, part, bodyweight) ->
                    db.execSQL("INSERT INTO exercise(name,name_key,body_part,category,is_bodyweight,is_deleted,created_at,updated_at) VALUES(?,?,?,'strength',?,0,0,0)",
                        arrayOf(name, Rules.nameKey(name), part, if (bodyweight) 1 else 0))
                }
            }
        }
    }
}

object InitialExercises {
    private val bodyweights = setOf("ディップス", "チンニング", "バックエクステンション", "クランチ", "レッグレイズ", "ハンギングレッグレイズ", "アブローラー")
    val values: List<Triple<String, String, Boolean>> = listOf(
        "chest" to "ベンチプレス,インクラインベンチプレス,ダンベルベンチプレス,インクラインダンベルプレス,ダンベルフライ,ケーブルクロスオーバー,チェストプレス,ペックフライ,ディップス",
        "back" to "デッドリフト,ベントオーバーロウ,ワンハンドダンベルロウ,ラットプルダウン,チンニング,シーテッドロウ,Tバーロウ,ケーブルプルオーバー,バックエクステンション,シュラッグ",
        "legs" to "バーベルスクワット,フロントスクワット,レッグプレス,レッグエクステンション,レッグカール,ルーマニアンデッドリフト,ブルガリアンスクワット,ヒップスラスト,カーフレイズ,ランジ",
        "shoulders" to "バーベルショルダープレス,ダンベルショルダープレス,サイドレイズ,フロントレイズ,リアレイズ,アップライトロウ,マシンショルダープレス,フェイスプル",
        "arms" to "バーベルカール,ダンベルカール,インクラインダンベルカール,ハンマーカール,プリーチャーカール,ケーブルカール,ナローベンチプレス,トライセプスプレスダウン,フレンチプレス,キックバック",
        "core" to "クランチ,ケーブルクランチ,レッグレイズ,ハンギングレッグレイズ,アブローラー,ロシアンツイスト"
    ).flatMap { (part, names) -> names.split(',').map { Triple(it, part, it in bodyweights) } }
}
