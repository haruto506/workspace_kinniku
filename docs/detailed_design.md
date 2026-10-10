# 筋トレ・運動記録アプリ 詳細設計書（ドラフト）

- 文書バージョン: 0.2（ドラフト）
- 作成日: 2026-09-05
- 対応する基本設計書: `docs/basic_design.md` v1.0
- 対応する要件定義書: `docs/requirements.md` v4.1
- 想定読者: 実装を担当するエンジニア、および本書をレビューする依頼者

## 0. この文書の読み方

### 0.1 記載方針

- 本書は基本設計書 v1.0 に定義された設計のみを詳細化する。基本設計に定義のない事項を独自に確定させることはしない。
- 基本設計に欠落・誤りを見つけた場合は、本書の本文で正しい設計を示したうえで、17 章「基本設計への改訂提案」に差分を記載する。基本設計書の改版をもって正式な確定とする。
- 基本設計 14 章の M 番号・15 章の Q 番号の暫定案は、0.4 の規則により本書で凍結する。凍結値は本文の記述に反映済みである。
- 詳細設計の過程で決定した事項は 16 章にまとめる。すべて「詳細確定」であり、実装を止めない。

### 0.2 本書のスコープ

| 含む | 含まない |
|---|---|
| クラス・インターフェースの責務と全公開メソッドのシグネチャ | メソッド本体の全行 |
| DAO のクエリ全文、Room アノテーション、戻り値の型 | Room が生成するコード |
| 画面ごとの `UiState` / `Event` / `Effect` の構造と ViewModel の処理 | Composable のピクセル単位のレイアウト、配色値 |
| 表示フォーマットの具体的な変換規則 | フォントサイズ、余白の数値 |
| CSV 出力のページング単位、MediaStore 操作手順、選択条件の全文 | ファイルシステムの実測性能値 |
| 単体・結合テストのテストケース一覧 | テストコードの本文 |
| マニフェスト宣言、通知チャンネル定義 | ビルド設定、署名、配布手順 |

### 0.3 決定状態の凡例

| 状態 | 意味 |
|---|---|
| 上位確定 | 要件定義書または基本設計書に明記されており、本書はそれを詳細化したもの |
| 詳細確定 | 基本設計の範囲内で詳細設計側が決定した。依頼者への報告対象だが、実装を止めない |
| 凍結値 | 基本設計の暫定案（M 番号・Q 番号）を、今回の実装における確定値として固定したもの（0.4） |
| 改訂提案 | 基本設計の記述に誤り・欠落があり、本書が異なる設計を採る。基本設計の改版が必要（17 章） |

本書に「承認待ち」の項目は存在しない。実装着手を待たせる項目を残さないためである。

### 0.4 承認待ち事項の凍結

基本設計書 v1.0 には、承認待ちの M 番号 20 件と Q 番号 14 件が残っている。基本設計 0.4 は「暫定案をそのまま採用して詳細設計を進める」と指示する一方で、Q-4、Q-5、Q-11、Q-14 について「実装着手前に確定させる」とも指示している。この 2 つは、実装担当者から見ると相反する指示になる。

本書は次の規則で状態を一意にする（詳細確定）。

1. M-1 から M-20、および Q-1 から Q-14 の暫定案を、**今回の実装における確定値**として凍結する。実装担当者はこれらを確定仕様として扱い、承認の有無を判断材料にしない。
2. 凍結値は 14 章に一覧化する。本文中の該当箇所には M 番号・Q 番号を付記し、由来を追跡できる状態に保つ。
3. 依頼者の回答が凍結値と異なった場合は、本書と基本設計書を改版し、15 章の追跡表に従って影響箇所を修正する。実装のやり直しはその改版をもって指示する。
4. 実装担当者の判断で凍結値を変更してはならない。凍結値に不都合を見つけた場合は、実装を止めずに報告する。

Q-4、Q-5、Q-11、Q-14 について実装着手前の確定を求める基本設計 0.4 の要請は、依頼者への確認依頼として有効である。ただしそれは文書側の手続きであり、実装の着手条件にはしない。この扱いを 17 章 R-7 として基本設計へ差し戻す。

### 0.5 実装担当者への指示

- 14 章の凍結値と 16 章の詳細確定事項を、確定仕様として実装する。独自の案に差し替えない。
- 本書に定義がない事項を独自の判断で補完しない。欠落や矛盾を見つけた場合は、仮置きせず報告する。
- 17 章の改訂提案は、基本設計書の記述と本書の記述が食い違う箇所である。実装は**本書に従う**。基本設計書の該当記述を根拠に実装しない。
- 15 章の追跡表は、凍結値が変わった場合に修正が必要となる箇所の一覧である。実装中も最新に保つ。

### 0.6 変更履歴

| 版 | 日付 | 内容 |
|---|---|---|
| 0.1 | 2026-09-05 | 初版ドラフト |
| 0.2 | 2026-09-05 | 独立レビューの指摘を反映。承認待ち事項を凍結値へ変更（0.4）。アラームを世代指定解除方式へ変更（5.3〜5.5）。`draftId` を廃し `recordId` 引き継ぎ方式へ変更（3.3、6.3.1）。集計 DAO を `Flow` 化（2.3.6、3.10）。全 Repository・全 DAO・全補助型の契約を追加。保存前の不変条件検証を追加（3.2）。CSV の回収条件・キャンセル・スナップショット範囲を明確化（7 章）。確認事項をすべて詳細確定へ変更（16 章）。基本設計への改訂提案を追加（17 章） |

---

## 1. モジュールと共通基盤

### 1.1 モジュール構成

単一モジュール（`app`）とする（詳細確定）。利用者 1 名・オフライン専用であり、ビルド時間の短縮効果よりも構成の単純さを優先する。基本設計 1.2 のレイヤ分離は、モジュール分割ではなくパッケージ分割で表現する。

### 1.2 ファイル構成

```
app/src/main/java/<package>/
 ├ MainActivity.kt                      単一 Activity。Navigation ホスト。タイマー復元の起点
 ├ KinnikuApplication.kt                @HiltAndroidApp。通知チャンネル作成、CSV 未完成ファイルの回収起動
 ├ di/
 │   ├ DatabaseModule.kt                AppDatabase と 10 DAO の提供
 │   ├ DataStoreModule.kt               Preferences DataStore の提供
 │   ├ RepositoryModule.kt              7 Repository のバインド
 │   ├ SystemModule.kt                  TimeProvider, DispatcherProvider, ApplicationScope,
 │   │                                  AlarmScheduler, RestNotifier, RestTimerCoordinator
 │   └ CsvModule.kt                     CsvExporter, MediaStoreGateway, PendingFileReaper
 ├ data/
 │   ├ db/
 │   │   ├ AppDatabase.kt
 │   │   ├ DatabaseCallback.kt          条件付き一意索引の作成、初期種目 53 件の投入
 │   │   ├ IndexDefinitions.kt          生 SQL による索引定義。Callback と Migration が共用
 │   │   ├ InitialExercises.kt          付録 A の定数配列
 │   │   ├ Converters.kt                BodyPart / ExerciseCategory の TypeConverter
 │   │   ├ entity/                      8 エンティティ
 │   │   ├ dao/                         10 DAO
 │   │   └ projection/                  クエリ戻り値のデータクラス
 │   ├ datastore/SettingsDataSource.kt
 │   ├ repository/                      7 Repository 実装
 │   ├ alarm/
 │   │   ├ AlarmSchedulerImpl.kt
 │   │   └ RestTimerAlarmReceiver.kt
 │   └ csv/
 │       ├ CsvExporter.kt               3 ファイルの生成制御
 │       ├ CsvSpec.kt                   ヘッダー定義とファイル名生成
 │       ├ CsvRowBuilder.kt             行の組み立てとエスケープ（純粋関数）
 │       ├ MediaStoreGateway.kt         MediaStore への登録・書き込み・確定・削除
 │       └ PendingFileReaper.kt         未完成ファイルの回収
 ├ domain/
 │   ├ model/                           ドメインモデルと補助型
 │   ├ usecase/                         ユースケース
 │   ├ calc/                            算出・検証・整形の純粋関数
 │   ├ timer/                           RestTimerCoordinator, AlarmScheduler, RestNotifier
 │   └ time/                            TimeProvider, DateChangeNotifier
 ├ ui/
 │   ├ theme/                           ライトテーマ定義
 │   ├ component/                       共通 UI 部品
 │   ├ format/                          表示フォーマッタ（純粋関数）
 │   ├ navigation/                      AppNavHost.kt, Routes.kt, NavResult.kt
 │   └ screen/today, picker, exercise, template, calendar, history,
 │            graph, weight, summary, csv, settings
 └ notification/
     ├ NotificationChannels.kt
     └ RestNotifierImpl.kt
```

### 1.3 エンティティと DAO の実数

レビュー指摘に従い、実数と名称を一覧で確定する（詳細確定）。

エンティティは 8 件。

| # | クラス | テーブル |
|---|---|---|
| 1 | `ExerciseEntity` | `exercise` |
| 2 | `SessionEntity` | `session` |
| 3 | `ExerciseRecordEntity` | `exercise_record` |
| 4 | `WorkoutSetEntity` | `workout_set` |
| 5 | `BodyWeightEntity` | `body_weight` |
| 6 | `TemplateEntity` | `template` |
| 7 | `TemplateItemEntity` | `template_item` |
| 8 | `RestTimerEntity` | `rest_timer` |

DAO は 11 件。すべて `AppDatabase` の抽象メソッドとして公開する。

| # | インターフェース | 主な対象 |
|---|---|---|
| 1 | `ExerciseDao` | 種目マスタ |
| 2 | `SessionDao` | セッション、実施日判定 |
| 3 | `RecordRangeDao` | 用途別の最古日・最新日 |
| 4 | `ExerciseRecordDao` | 種目記録、親子の文脈取得 |
| 5 | `WorkoutSetDao` | セット |
| 6 | `BodyWeightDao` | 体重 |
| 7 | `VolumeDao` | 総ボリューム・部位別・種目別の集計 |
| 8 | `TemplateDao` | テンプレート |
| 9 | `TemplateItemDao` | テンプレート明細 |
| 10 | `RestTimerDao` | 休憩タイマー状態 |
| 11 | `CsvDao` | CSV 出力用のページング読み取り |

### 1.4 TimeProvider

```kotlin
interface TimeProvider {
    fun today(): LocalDate                 // 端末ローカルタイムゾーンの当日
    fun zoneId(): ZoneId
    fun nowEpochMillis(): Long             // System.currentTimeMillis()
    fun nowLocalDateTime(): LocalDateTime   // CSV のファイル名に用いる
    fun elapsedRealtimeMillis(): Long      // SystemClock.elapsedRealtime()
    fun bootCount(): Int                   // Settings.Global.BOOT_COUNT。取得不可は BOOT_COUNT_UNKNOWN

    companion object { const val BOOT_COUNT_UNKNOWN = -1 }
}
```

`bootCount()` の扱い（改訂提案 R-4。基本設計 5.2.1 に規定がない）。

- 取得に失敗した場合は `BOOT_COUNT_UNKNOWN`（`-1`）を返す。
- **保存値と現在値のどちらかが `BOOT_COUNT_UNKNOWN` であれば、比較結果によらず再起動とみなしてタイマーを破棄する**（本書 5.5）。
- 両方が `-1` のときに「一致した」と判定してはならない。`-1` は値ではなく「判定不能」を表す。判定不能な状態で計測を継続すると、再起動をまたいだ経過時間を休憩時間として記録しうる。
- タイマー開始時に `bootCount()` が `-1` を返した場合も、その値をそのまま `started_boot_count` に保存する。復元時に必ず破棄される。計測自体は開始し、前面にいる間はフォアグラウンド監視で確定できる。

### 1.5 DispatcherProvider と ApplicationScope

```kotlin
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
    val csv: CoroutineDispatcher      // Dispatchers.IO.limitedParallelism(1)
}

@Qualifier annotation class ApplicationScope
```

- `csv` は CSV 出力と未完成ファイルの回収を 1 本に直列化する。両者が同じエントリを取り合わないようにするため（基本設計 8.8、本書 7.6）。
- `@ApplicationScope CoroutineScope` は `CoroutineScope(SupervisorJob() + dispatchers.io)` として `SystemModule` が提供する。`KinnikuApplication` からの回収起動に用いる。画面の生存期間に縛られない処理の唯一の起点とする（詳細確定）。

### 1.6 エラーモデルと読み取り結果

```kotlin
sealed interface AppResult<out T> {
    data class Success<T>(val value: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

sealed interface AppError {
    data class Validation(val field: FieldId, val kind: ValidationKind) : AppError
    data object DuplicateExerciseName : AppError
    data object PastDayNotEditable : AppError      // 基本設計 4.1
    data object TargetGone : AppError              // 対象行が存在しない
    data object CategoryMismatch : AppError        // 筋トレ／有酸素の不整合
    data object WeightInconsistent : AppError      // 自重フラグと重量 NULL の不整合
    data object TimerNotReady : AppError           // 復元完了前の操作（本書 5.8）
    data class Unexpected(val cause: Throwable) : AppError
}
```

読み取り系の `Flow` は例外を流さず、次の型を流す（詳細確定。レビュー指摘 10.2 に対応）。

```kotlin
sealed interface LoadResult<out T> {
    data object Loading : LoadResult<Nothing>
    data class Ready<T>(val value: T) : LoadResult<T>
    data class Failed<T>(val error: AppError, val lastValue: T?) : LoadResult<T>
}
```

- 読み取りに失敗した場合、空の一覧を流してはならない。「記録がない」と「読み取れなかった」を UI が区別できなくなるためである。
- `Failed` には直前に成功した値を `lastValue` として添える。UI は最後の正常値を表示したまま、エラー表示を重ねる。
- `TypeConverter` の未知コード（DB の破損）は `Failed` として扱う。空状態へ変換しない。
- Repository は `Flow` の内部で `catch` し、`LoadResult.Failed` を発行する。「エラーを別チャネルへ送る」経路は設けない。

### 1.7 共通の補助型

レビュー指摘に従い、本書で用いる補助型をすべてここで定義する。

```kotlin
@JvmInline value class DraftId(val value: String)      // UUID 文字列。DB へ保存しない

enum class MoveDirection { UP, DOWN }

enum class FieldId {
    EXERCISE_NAME, SESSION_NAME, TEMPLATE_NAME, MEMO,
    WEIGHT, REPS, DURATION_MIN, BODY_WEIGHT, REST_TARGET_SEC,
    TEMPLATE_SET_COUNT, CSV_FROM_DATE, CSV_TO_DATE,
}

sealed interface ValidationKind {
    data object Required : ValidationKind
    data object OutOfRange : ValidationKind
    data object BadStep : ValidationKind
    data object TooLong : ValidationKind
    data object BlankOnly : ValidationKind
    data object NotANumber : ValidationKind
    data object RangeReversed : ValidationKind          // CSV の開始日 > 終了日
}

data class ExerciseInput(
    val name: String,
    val bodyPart: BodyPart,
    val category: ExerciseCategory,
    val isBodyweight: Boolean,
)

data class SetInput(
    val weightText: String = "",
    val repsText: String = "",
    val memoText: String = "",
)

sealed interface SessionTarget {                       // 基本設計 3.2.1
    data class Existing(val sessionId: Long) : SessionTarget
    data object AutoCreateOnCommit : SessionTarget
}

data class ExerciseSnapshot(
    val exerciseId: Long, val name: String, val bodyPart: BodyPart,
    val category: ExerciseCategory, val isBodyweight: Boolean,
)

sealed interface TimerCancellation {
    data object None : TimerCancellation
    data class Cancel(val token: Long, val targetSetId: Long) : TimerCancellation
}
```

- `ExerciseSnapshot` は UI から Repository へ渡さない。Repository がトランザクション内で種目マスタから生成する（本書 3.3）。UI 層では表示用としてのみ用いる。
- `TimerCancellation` は解除すべき世代を持つ。アラームの解除は世代指定で行うため（本書 5.3）。

---

## 2. データ層詳細設計

### 2.1 エンティティ定義

列名は基本設計 2.4 の定義に一致させ、Kotlin 側のプロパティ名はキャメルケースとする。

#### 2.1.1 ExerciseEntity

```kotlin
@Entity(tableName = "exercise", indices = [Index("is_deleted")])
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "name_key") val nameKey: String,
    @ColumnInfo(name = "body_part") val bodyPart: BodyPart,
    val category: ExerciseCategory,
    @ColumnInfo(name = "is_bodyweight") val isBodyweight: Boolean,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
```

#### 2.1.2 SessionEntity

```kotlin
@Entity(tableName = "session", indices = [Index("epoch_day")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    val name: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

#### 2.1.3 ExerciseRecordEntity

```kotlin
@Entity(
    tableName = "exercise_record",
    indices = [Index("session_id"), Index("exercise_id")],
    foreignKeys = [
        ForeignKey(SessionEntity::class, ["id"], ["session_id"], onDelete = CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exercise_id"], onDelete = NO_ACTION),
    ]
)
data class ExerciseRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "snapshot_name") val snapshotName: String,
    @ColumnInfo(name = "snapshot_body_part") val snapshotBodyPart: BodyPart,
    @ColumnInfo(name = "snapshot_category") val snapshotCategory: ExerciseCategory,
    @ColumnInfo(name = "snapshot_is_bodyweight") val snapshotIsBodyweight: Boolean,
    @ColumnInfo(name = "duration_min") val durationMin: Int?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

#### 2.1.4 WorkoutSetEntity

```kotlin
@Entity(
    tableName = "workout_set",
    indices = [Index("exercise_record_id")],
    foreignKeys = [
        ForeignKey(ExerciseRecordEntity::class, ["id"], ["exercise_record_id"], onDelete = CASCADE),
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "exercise_record_id") val exerciseRecordId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "weight_tenth_kg") val weightTenthKg: Int?,
    val reps: Int,
    @ColumnInfo(name = "rest_sec") val restSec: Int?,
    val memo: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

#### 2.1.5 BodyWeightEntity / TemplateEntity / TemplateItemEntity

```kotlin
@Entity(tableName = "body_weight", indices = [Index("epoch_day")])
data class BodyWeightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "weight_tenth_kg") val weightTenthKg: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(tableName = "template")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
    tableName = "template_item",
    indices = [Index("template_id"), Index("exercise_id")],
    foreignKeys = [
        ForeignKey(TemplateEntity::class, ["id"], ["template_id"], onDelete = CASCADE),
        ForeignKey(ExerciseEntity::class, ["id"], ["exercise_id"], onDelete = NO_ACTION),
    ]
)
data class TemplateItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "template_id") val templateId: Long,
    @ColumnInfo(name = "exercise_id") val exerciseId: Long,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "set_count") val setCount: Int?,
)
```

#### 2.1.6 RestTimerEntity

```kotlin
@Entity(
    tableName = "rest_timer",
    foreignKeys = [
        ForeignKey(WorkoutSetEntity::class, ["id"], ["target_set_id"], onDelete = CASCADE),
    ]
)
data class RestTimerEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,          // 常に 1
    val token: Long,
    @ColumnInfo(name = "target_set_id") val targetSetId: Long,
    @ColumnInfo(name = "started_elapsed_millis") val startedElapsedMillis: Long,
    @ColumnInfo(name = "started_boot_count") val startedBootCount: Int,
    @ColumnInfo(name = "started_at_millis") val startedAtMillis: Long,
    @ColumnInfo(name = "target_sec") val targetSec: Int,
) {
    companion object { const val SINGLETON_ID = 1 }
}
```

- `id` は自動採番しない。挿入時に必ず `SINGLETON_ID` を渡し、`OnConflictStrategy.ABORT` とする。旧行の削除漏れを実行時に検出するため（詳細確定）。

### 2.2 列挙と TypeConverter

```kotlin
enum class BodyPart(val code: String) {
    CHEST("chest"), BACK("back"), LEGS("legs"),
    SHOULDERS("shoulders"), ARMS("arms"), CORE("core"), CARDIO("cardio");

    companion object {
        val STRENGTH_PARTS: List<BodyPart> = entries - CARDIO      // 期間サマリーの 6 部位
        fun fromCode(code: String): BodyPart =
            entries.firstOrNull { it.code == code }
                ?: error("unknown BodyPart code: $code")
    }
}

enum class ExerciseCategory(val code: String) {
    STRENGTH("strength"), CARDIO("cardio");

    companion object {
        fun fromCode(code: String): ExerciseCategory =
            entries.firstOrNull { it.code == code }
                ?: error("unknown ExerciseCategory code: $code")
    }
}
```

- 宣言順が表示順・集計順になる（基本設計 2.3）。
- `fromCode` の失敗は `IllegalStateException` とする。DB の破損を意味するため握り潰さない。メッセージに読み取ったコードを含める。この例外は `LoadResult.Failed` として UI へ届き、空状態へは変換しない（本書 1.6）。
- 表示名は `strings.xml` に置き、`BodyPart` から文字列リソース ID への対応表を UI 層に持つ。DB とドメイン層は日本語文字列を持たない（基本設計 2.3）。

### 2.3 クエリ戻り値のデータクラス

`data/db/projection` に置く。レビュー指摘に従い、本書で参照するすべての射影型を定義する。

```kotlin
data class ExerciseWithUsage(
    @Embedded val exercise: ExerciseEntity,
    @ColumnInfo(name = "usage_count") val usageCount: Long,
)

data class RecordRow(
    @Embedded val record: ExerciseRecordEntity,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "session_name") val sessionName: String?,
    @ColumnInfo(name = "session_created_at") val sessionCreatedAt: Long,
)

/** セット ID または種目記録 ID から、親の文脈をまとめて引く。保存前の検証に用いる（3.2） */
data class RecordContext(
    @ColumnInfo(name = "record_id") val recordId: Long,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "snapshot_category") val snapshotCategory: ExerciseCategory,
    @ColumnInfo(name = "snapshot_is_bodyweight") val snapshotIsBodyweight: Boolean,
)

data class DailyWeightTotal(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "sum_tenth") val sumTenth: Long,
    @ColumnInfo(name = "count") val count: Int,
)

data class DailyPartVolume(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "body_part") val bodyPart: BodyPart,
    @ColumnInfo(name = "volume_tenth") val volumeTenth: Long,
)

data class DailyPartReps(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "body_part") val bodyPart: BodyPart,
    @ColumnInfo(name = "reps_total") val repsTotal: Long,
)

data class ExerciseDailyRow(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "is_bodyweight") val isBodyweight: Boolean,
    @ColumnInfo(name = "value") val value: Long,   // 自重は回数合計、非自重はボリューム
)

data class TemplateItemRow(
    @Embedded val item: TemplateItemEntity,
    @ColumnInfo(name = "exercise_name") val exerciseName: String,
    @ColumnInfo(name = "exercise_body_part") val exerciseBodyPart: BodyPart,
    @ColumnInfo(name = "exercise_category") val exerciseCategory: ExerciseCategory,
    @ColumnInfo(name = "exercise_is_bodyweight") val exerciseIsBodyweight: Boolean,
)

/** 削除操作の前に、解除すべきタイマー世代を引く */
data class TimerGeneration(
    val token: Long,
    @ColumnInfo(name = "target_set_id") val targetSetId: Long,
)
```

### 2.4 DAO 定義

戻り値の型は基本設計 1.5 の数値型規約に従う。`SUM` の結果はすべて `Long?` で受け、Repository で 0 に置き換える。

画面が購読する参照系はすべて `Flow` を返す。1 回きりの取得（`suspend`）を返してよいのは、トランザクション内で呼ぶものと、算出の材料を都度取得するものに限る。

#### 2.4.1 ExerciseDao

```kotlin
@Dao
interface ExerciseDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entities: List<ExerciseEntity>): List<Long>

    @Query("SELECT * FROM exercise WHERE is_deleted = 0 ORDER BY id ASC")
    fun observeActive(): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT e.*,
               (SELECT COUNT(*) FROM exercise_record r WHERE r.exercise_id = e.id) AS usage_count
        FROM exercise e
        WHERE e.is_deleted = 0
        ORDER BY usage_count DESC, e.id ASC
    """)
    fun observeActiveWithUsage(): Flow<List<ExerciseWithUsage>>

    @Query("""
        SELECT e.* FROM exercise e
        WHERE EXISTS (
            SELECT 1 FROM exercise_record r
            WHERE r.exercise_id = e.id AND r.snapshot_category = 'strength'
        )
        ORDER BY e.id ASC
    """)
    fun observeGraphSelectable(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun findById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercise WHERE name_key = :nameKey AND is_deleted = 0 LIMIT 1")
    suspend fun findActiveByNameKey(nameKey: String): ExerciseEntity?

    @Query("""
        UPDATE exercise
        SET name = :name, name_key = :nameKey, body_part = :bodyPart,
            category = :category, is_bodyweight = :isBodyweight, updated_at = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateAttributes(
        id: Long, name: String, nameKey: String, bodyPart: BodyPart,
        category: ExerciseCategory, isBodyweight: Boolean, updatedAt: Long,
    ): Int

    @Query("UPDATE exercise SET is_deleted = 1, updated_at = :updatedAt WHERE id = :id AND is_deleted = 0")
    suspend fun markDeleted(id: Long, updatedAt: Long): Int
}
```

- `observeGraphSelectable` の `EXISTS` に `snapshot_category = 'strength'` を含める（改訂提案 R-6）。集計クエリ（2.4.6）が筋トレのみを対象とするため、有酸素の記録しか持たない種目を選択肢に出すと必ず空グラフになる。基本設計 S-07 は「過去記録が存在する種目」と書いているが、その文言のままでは操作できない選択肢が並ぶ。
- `markDeleted` の条件に `is_deleted = 0` を含める。戻り値が 0 のとき、対象が存在しないか既に削除済みであることが分かる。Repository はこれを `AppError.TargetGone` へ変換する。
- `usage_count` は `Long` で受ける。想定規模（M-11）では `Int` に収まるが、集計値の型を `Long` に統一する規約（基本設計 1.5）に合わせる。
- `observeActiveWithUsage` の相関副問い合わせは `exercise_record(exercise_id)` の索引を用いる。有効種目は多くても数百件であり、副問い合わせの反復回数は小さい。

#### 2.4.2 SessionDao

```kotlin
@Dao
interface SessionDao {
    @Insert suspend fun insert(entity: SessionEntity): Long

    @Query("SELECT * FROM session WHERE epoch_day = :epochDay ORDER BY created_at ASC, id ASC")
    fun observeByDay(epochDay: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE epoch_day = :epochDay ORDER BY created_at ASC, id ASC")
    suspend fun findByDay(epochDay: Long): List<SessionEntity>

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun findById(id: Long): SessionEntity?

    @Query("UPDATE session SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String?): Int

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("""
        SELECT DISTINCT s.epoch_day
        FROM session s
        JOIN exercise_record r ON r.session_id = s.id
        WHERE s.epoch_day BETWEEN :from AND :to
          AND ( r.snapshot_category = 'cardio'
                OR EXISTS (SELECT 1 FROM workout_set w WHERE w.exercise_record_id = r.id) )
    """)
    fun observeTrainingDays(from: Long, to: Long): Flow<List<Long>>
}
```

- `observeTrainingDays` は要件 4.7 の定義をそのまま表す。セットが 0 件の筋トレ種目記録は `EXISTS` が偽となり、実施日に数えない（基本設計 3.5、Q-11）。
- `snapshot_category = 'cardio'` の行は `duration_min` が非 NULL であることが不変条件（基本設計 3.5）のため、実施時間の存在を別途確認しない。

#### 2.4.3 RecordRangeDao（最古日・最新日）

レビュー指摘に従い、用途ごとに別のクエリを定義する（詳細確定）。空セッションを「記録あり」と数える 1 本のクエリを共用しない。

```kotlin
@Dao
interface RecordRangeDao {
    /** CSV の期間既定値。筋トレ・有酸素・体重のいずれかが存在する日 */
    @Query("""
        SELECT MIN(d) FROM (
            SELECT MIN(s.epoch_day) AS d FROM session s
              JOIN exercise_record r ON r.session_id = s.id
            UNION ALL
            SELECT MIN(epoch_day) AS d FROM body_weight
        )
    """)
    suspend fun csvOldestDay(): Long?

    @Query("""
        SELECT MAX(d) FROM (
            SELECT MAX(s.epoch_day) AS d FROM session s
              JOIN exercise_record r ON r.session_id = s.id
            UNION ALL
            SELECT MAX(epoch_day) AS d FROM body_weight
        )
    """)
    suspend fun csvNewestDay(): Long?

    /** 期間サマリーの前方向移動の下限。総ボリュームに寄与するセットが存在する日 */
    @Query("""
        SELECT MIN(s.epoch_day)
        FROM workout_set w
        JOIN exercise_record r ON r.id = w.exercise_record_id
        JOIN session s ON s.id = r.session_id
        WHERE r.snapshot_category = 'strength'
    """)
    suspend fun summaryOldestDay(): Long?

    /** 種目別グラフの「全期間」の開始日 */
    @Query("""
        SELECT MIN(s.epoch_day)
        FROM workout_set w
        JOIN exercise_record r ON r.id = w.exercise_record_id
        JOIN session s ON s.id = r.session_id
        WHERE r.exercise_id = :exerciseId AND r.snapshot_category = 'strength'
    """)
    suspend fun exerciseOldestDay(exerciseId: Long): Long?
}
```

- CSV は空セッションを出力しないため、`csvOldestDay` は `exercise_record` との結合を必須とする。
- 期間サマリーは総ボリュームのみを扱うため、`summaryOldestDay` は `workout_set` の存在を必須とする。有酸素だけの日を下限にしない。
- グラフの「全期間」は種目ごとに異なるため、`exerciseOldestDay` を別に持つ。

#### 2.4.4 ExerciseRecordDao

```kotlin
@Dao
interface ExerciseRecordDao {
    @Insert suspend fun insert(entity: ExerciseRecordEntity): Long

    @Query("""
        SELECT r.*, s.epoch_day AS epoch_day, s.name AS session_name,
               s.created_at AS session_created_at
        FROM exercise_record r
        JOIN session s ON s.id = r.session_id
        WHERE s.epoch_day = :epochDay
        ORDER BY s.created_at ASC, s.id ASC, r.order_index ASC, r.id ASC
    """)
    fun observeByDay(epochDay: Long): Flow<List<RecordRow>>

    @Query("SELECT COALESCE(MAX(order_index), -1) + 1 FROM exercise_record WHERE session_id = :sessionId")
    suspend fun nextOrderIndex(sessionId: Long): Int

    @Query("SELECT id FROM exercise_record WHERE session_id = :sessionId ORDER BY order_index ASC, id ASC")
    suspend fun orderedIds(sessionId: Long): List<Long>

    @Query("UPDATE exercise_record SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int): Int

    @Query("UPDATE exercise_record SET duration_min = :durationMin WHERE id = :id")
    suspend fun updateDurationMin(id: Long, durationMin: Int): Int

    @Query("DELETE FROM exercise_record WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** 種目記録 ID から親の文脈を引く。保存前の検証に用いる（3.2） */
    @Query("""
        SELECT r.id AS record_id, r.session_id AS session_id, s.epoch_day AS epoch_day,
               r.snapshot_category AS snapshot_category,
               r.snapshot_is_bodyweight AS snapshot_is_bodyweight
        FROM exercise_record r JOIN session s ON s.id = r.session_id
        WHERE r.id = :recordId
    """)
    suspend fun findContextByRecordId(recordId: Long): RecordContext?

    /** セット ID から親の文脈を引く */
    @Query("""
        SELECT r.id AS record_id, r.session_id AS session_id, s.epoch_day AS epoch_day,
               r.snapshot_category AS snapshot_category,
               r.snapshot_is_bodyweight AS snapshot_is_bodyweight
        FROM workout_set w
        JOIN exercise_record r ON r.id = w.exercise_record_id
        JOIN session s ON s.id = r.session_id
        WHERE w.id = :setId
    """)
    suspend fun findContextBySetId(setId: Long): RecordContext?
}
```

- `findContextBySetId` と `findContextByRecordId` は、Repository が保存直前に当日・区分・自重フラグを再確認するための唯一の経路である（本書 3.2）。UI から渡された値を検証の根拠にしない。
- `nextOrderIndex` は `order_index` が 0 起点であるため `COALESCE(MAX(...), -1) + 1` とする（基本設計 2.6）。

#### 2.4.5 WorkoutSetDao

```kotlin
@Dao
interface WorkoutSetDao {
    @Insert suspend fun insert(entity: WorkoutSetEntity): Long

    @Query("""
        SELECT * FROM workout_set
        WHERE exercise_record_id IN (:recordIds)
        ORDER BY exercise_record_id ASC, order_index ASC, id ASC
    """)
    fun observeByRecordIds(recordIds: List<Long>): Flow<List<WorkoutSetEntity>>

    @Query("SELECT COALESCE(MAX(order_index), -1) + 1 FROM workout_set WHERE exercise_record_id = :recordId")
    suspend fun nextOrderIndex(recordId: Long): Int

    @Query("SELECT id FROM workout_set WHERE exercise_record_id = :recordId ORDER BY order_index ASC, id ASC")
    suspend fun orderedIds(recordId: Long): List<Long>

    @Query("UPDATE workout_set SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int): Int

    @Query("UPDATE workout_set SET weight_tenth_kg = :weightTenthKg, reps = :reps WHERE id = :id")
    suspend fun updateWeightAndReps(id: Long, weightTenthKg: Int?, reps: Int): Int

    @Query("UPDATE workout_set SET rest_sec = :restSec WHERE id = :id")
    suspend fun updateRestSec(id: Long, restSec: Int?): Int

    @Query("SELECT COUNT(*) FROM workout_set WHERE exercise_record_id = :recordId")
    suspend fun countByRecord(recordId: Long): Int

    @Query("DELETE FROM workout_set WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
```

- `observeByRecordIds` に空リストを渡すと SQLite の `IN ()` が構文誤りになるため、Repository は空リストの場合にクエリを呼ばず `emptyList()` を返す（詳細確定）。
- `updateWeightAndReps` はメモを含めない。要件 4.3.1 が編集可能項目を限定しており、メモは対象外である（16 章 D-1）。

#### 2.4.6 VolumeDao

自重種目は日ごとの体重解決を要するため、SQL では合計せず「回数の合計」までを求め、Kotlin 側で体重を掛ける（基本設計 4.3.1）。

集計クエリはすべて `Flow` を返す。Room の `Flow` は、クエリが参照するテーブルのいずれかが変更されると再実行される。ここでは `session`・`exercise_record`・`workout_set` の 3 テーブルを参照するため、記録の追加・編集・削除で自動的に再計算される（レビュー指摘に対応。詳細確定）。

```kotlin
@Dao
interface VolumeDao {
    @Query("""
        SELECT s.epoch_day AS epoch_day,
               r.snapshot_body_part AS body_part,
               SUM(CAST(w.weight_tenth_kg AS INTEGER) * w.reps) AS volume_tenth
        FROM workout_set w
        JOIN exercise_record r ON r.id = w.exercise_record_id
        JOIN session s ON s.id = r.session_id
        WHERE s.epoch_day BETWEEN :from AND :to
          AND r.snapshot_category = 'strength'
          AND r.snapshot_is_bodyweight = 0
          AND w.weight_tenth_kg IS NOT NULL
        GROUP BY s.epoch_day, r.snapshot_body_part
    """)
    fun observeDailyPartVolumeNonBodyweight(from: Long, to: Long): Flow<List<DailyPartVolume>>

    @Query("""
        SELECT s.epoch_day AS epoch_day,
               r.snapshot_body_part AS body_part,
               SUM(w.reps) AS reps_total
        FROM workout_set w
        JOIN exercise_record r ON r.id = w.exercise_record_id
        JOIN session s ON s.id = r.session_id
        WHERE s.epoch_day BETWEEN :from AND :to
          AND r.snapshot_category = 'strength'
          AND r.snapshot_is_bodyweight = 1
        GROUP BY s.epoch_day, r.snapshot_body_part
    """)
    fun observeDailyPartRepsBodyweight(from: Long, to: Long): Flow<List<DailyPartReps>>

    @Query("""
        SELECT s.epoch_day AS epoch_day,
               r.snapshot_is_bodyweight AS is_bodyweight,
               SUM(CASE WHEN r.snapshot_is_bodyweight = 1
                        THEN w.reps
                        ELSE CAST(w.weight_tenth_kg AS INTEGER) * w.reps END) AS value
        FROM workout_set w
        JOIN exercise_record r ON r.id = w.exercise_record_id
        JOIN session s ON s.id = r.session_id
        WHERE r.exercise_id = :exerciseId
          AND r.snapshot_category = 'strength'
          AND s.epoch_day BETWEEN :from AND :to
        GROUP BY s.epoch_day, r.snapshot_is_bodyweight
        ORDER BY s.epoch_day ASC
    """)
    fun observeExerciseDaily(exerciseId: Long, from: Long, to: Long): Flow<List<ExerciseDailyRow>>
}
```

- 同一の種目 ID でも、自重フラグを途中で変更すると記録ごとに `snapshot_is_bodyweight` が異なる。そのため `is_bodyweight` を集計キーに含め、Kotlin 側で日ごとに合算する（詳細確定）。
- `is_bodyweight = 1` の行の `value` は回数合計、`0` の行の `value` はボリューム（0.1 kg 単位）である。意味が異なるため、Repository で必ず分岐して扱う。
- `CAST(... AS INTEGER)` は SQLite では 64 ビット整数となる。乗算の途中で 32 ビットに丸められることはない。
- 種目別グラフは `snapshot_category = 'strength'` に限る。有酸素は総ボリュームを持たない（要件 4.8）。

#### 2.4.7 BodyWeightDao

```kotlin
@Dao
interface BodyWeightDao {
    @Insert suspend fun insert(entity: BodyWeightEntity): Long

    @Query("""
        SELECT * FROM body_weight
        WHERE epoch_day BETWEEN :from AND :to
        ORDER BY epoch_day ASC, created_at ASC, id ASC
    """)
    fun observeByRange(from: Long, to: Long): Flow<List<BodyWeightEntity>>

    @Query("""
        SELECT epoch_day AS epoch_day, SUM(weight_tenth_kg) AS sum_tenth, COUNT(*) AS count
        FROM body_weight
        WHERE epoch_day BETWEEN :from AND :to
        GROUP BY epoch_day
        ORDER BY epoch_day ASC
    """)
    suspend fun dailyTotals(from: Long, to: Long): List<DailyWeightTotal>

    @Query("SELECT MAX(epoch_day) FROM body_weight WHERE epoch_day < :day")
    suspend fun latestDayBefore(day: Long): Long?

    @Query("""
        SELECT epoch_day AS epoch_day, SUM(weight_tenth_kg) AS sum_tenth, COUNT(*) AS count
        FROM body_weight WHERE epoch_day = :day
    """)
    suspend fun totalForDay(day: Long): DailyWeightTotal?

    @Query("SELECT * FROM body_weight WHERE id = :id")
    suspend fun findById(id: Long): BodyWeightEntity?

    @Query("UPDATE body_weight SET weight_tenth_kg = :weightTenthKg WHERE id = :id")
    suspend fun update(id: Long, weightTenthKg: Int): Int

    @Query("DELETE FROM body_weight WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    /** 体重テーブルの変更を検知するための信号。値そのものに意味はない */
    @Query("SELECT COUNT(*) FROM body_weight")
    fun observeRevision(): Flow<Int>
}
```

- `observeRevision` は、`body_weight` テーブルが変更されたことを `VolumeRepository` へ伝えるためだけのクエリである（詳細確定）。Room の `Flow` はテーブルの無効化ごとに再実行され、値が同じでも再発行するため、変更の信号として機能する。件数が変わらない更新（体重値の書き換え）でも発火する。
- `dailyTotals` と `totalForDay` は `suspend` とする。呼び出し側は `observeRevision` で再取得の契機を得る（本書 3.10）。
- `latestDayBefore` と `totalForDay` の 2 本で、基本設計 4.3.1 が求める「期間開始日より前の直近日の全記録」を取得する。1 件だけを取る問い合わせは定義しない。
- `totalForDay` は対象日に記録がない場合、`sum_tenth` が NULL、`count` が 0 の行を返す。Repository は `count == 0` を「記録なし」として扱う。

#### 2.4.8 TemplateDao / TemplateItemDao

```kotlin
@Dao
interface TemplateDao {
    @Insert suspend fun insert(entity: TemplateEntity): Long

    @Query("SELECT * FROM template ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM template WHERE id = :id")
    suspend fun findById(id: Long): TemplateEntity?

    @Query("UPDATE template SET name = :name, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateName(id: Long, name: String, updatedAt: Long): Int

    @Query("DELETE FROM template WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}

@Dao
interface TemplateItemDao {
    @Insert suspend fun insert(entity: TemplateItemEntity): Long
    @Insert suspend fun insertAll(entities: List<TemplateItemEntity>): List<Long>

    @Query("""
        SELECT t.*, e.name AS exercise_name, e.body_part AS exercise_body_part,
               e.category AS exercise_category, e.is_bodyweight AS exercise_is_bodyweight
        FROM template_item t JOIN exercise e ON e.id = t.exercise_id
        ORDER BY t.template_id ASC, t.order_index ASC, t.id ASC
    """)
    fun observeAllRows(): Flow<List<TemplateItemRow>>

    @Query("""
        SELECT t.*, e.name AS exercise_name, e.body_part AS exercise_body_part,
               e.category AS exercise_category, e.is_bodyweight AS exercise_is_bodyweight
        FROM template_item t JOIN exercise e ON e.id = t.exercise_id
        WHERE t.template_id = :templateId
        ORDER BY t.order_index ASC, t.id ASC
    """)
    suspend fun findRowsByTemplate(templateId: Long): List<TemplateItemRow>

    @Query("SELECT COALESCE(MAX(order_index), -1) + 1 FROM template_item WHERE template_id = :templateId")
    suspend fun nextOrderIndex(templateId: Long): Int

    @Query("SELECT DISTINCT template_id FROM template_item WHERE exercise_id = :exerciseId")
    suspend fun affectedTemplateIds(exerciseId: Long): List<Long>

    @Query("DELETE FROM template_item WHERE exercise_id = :exerciseId")
    suspend fun deleteByExercise(exerciseId: Long): Int

    @Query("DELETE FROM template_item WHERE template_id = :templateId")
    suspend fun deleteByTemplate(templateId: Long): Int

    @Query("DELETE FROM template_item WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("SELECT id FROM template_item WHERE template_id = :templateId ORDER BY order_index ASC, id ASC")
    suspend fun orderedIds(templateId: Long): List<Long>

    @Query("UPDATE template_item SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int): Int

    @Query("UPDATE template_item SET set_count = :setCount WHERE id = :id")
    suspend fun updateSetCount(id: Long, setCount: Int?): Int
}
```

- `observeAllRows` は種目マスタを結合する。テンプレートはスナップショットを持たないため、種目名の変更が一覧へ即座に反映される（基本設計 2.4.6）。
- 削除済みの種目を参照する明細は存在しない。種目の削除時に必ず取り除かれるため（基本設計 4.6、本書 3.6）。

#### 2.4.9 RestTimerDao

```kotlin
@Dao
interface RestTimerDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RestTimerEntity): Long

    @Query("SELECT * FROM rest_timer WHERE id = 1")
    suspend fun get(): RestTimerEntity?

    @Query("SELECT * FROM rest_timer WHERE id = 1")
    fun observe(): Flow<RestTimerEntity?>

    @Query("DELETE FROM rest_timer WHERE id = 1 AND token = :token AND target_set_id = :targetSetId")
    suspend fun deleteMatching(token: Long, targetSetId: Long): Int

    /** 削除対象の直前に、解除すべき世代を引く */
    @Query("SELECT token, target_set_id FROM rest_timer WHERE id = 1 AND target_set_id = :setId")
    suspend fun generationForSet(setId: Long): TimerGeneration?

    @Query("""
        SELECT t.token, t.target_set_id FROM rest_timer t
        JOIN workout_set w ON w.id = t.target_set_id
        WHERE w.exercise_record_id = :recordId
    """)
    suspend fun generationForRecord(recordId: Long): TimerGeneration?

    @Query("""
        SELECT t.token, t.target_set_id FROM rest_timer t
        JOIN workout_set w ON w.id = t.target_set_id
        JOIN exercise_record r ON r.id = w.exercise_record_id
        WHERE r.session_id = :sessionId
    """)
    suspend fun generationForSession(sessionId: Long): TimerGeneration?
}
```

- `deleteMatching` の戻り値（削除件数）が、どの経路が完了させたかを決める唯一の根拠である（基本設計 5.1.2）。
- `generationForXxx` の 3 本は、削除操作の前に解除すべきアラーム世代を取得するためのものである。世代を持たないと、固定の予約を無条件に解除して新しいタイマーを壊す（本書 5.3）。

#### 2.4.10 CsvDao

キーセット方式のページングとする（基本設計 8.8）。`OFFSET` は用いない。

```kotlin
@Dao
interface CsvDao {
    /** 出力開始時に上限キーを固定する。開始後に追加された行を出力しない（7.2） */
    @Query("""
        SELECT * FROM session
        WHERE epoch_day BETWEEN :from AND :to
          AND ( epoch_day > :lastDay
                OR (epoch_day = :lastDay AND created_at > :lastCreatedAt)
                OR (epoch_day = :lastDay AND created_at = :lastCreatedAt AND id > :lastId) )
          AND ( epoch_day < :maxDay
                OR (epoch_day = :maxDay AND created_at < :maxCreatedAt)
                OR (epoch_day = :maxDay AND created_at = :maxCreatedAt AND id <= :maxId) )
        ORDER BY epoch_day ASC, created_at ASC, id ASC
        LIMIT :limit
    """)
    suspend fun sessionPage(
        from: Long, to: Long,
        lastDay: Long, lastCreatedAt: Long, lastId: Long,
        maxDay: Long, maxCreatedAt: Long, maxId: Long,
        limit: Int,
    ): List<SessionEntity>

    @Query("""
        SELECT * FROM exercise_record
        WHERE session_id IN (:sessionIds)
        ORDER BY session_id ASC, order_index ASC, id ASC
    """)
    suspend fun recordsForSessions(sessionIds: List<Long>): List<ExerciseRecordEntity>

    @Query("""
        SELECT * FROM workout_set
        WHERE exercise_record_id IN (:recordIds)
        ORDER BY exercise_record_id ASC, order_index ASC, id ASC
    """)
    suspend fun setsForRecords(recordIds: List<Long>): List<WorkoutSetEntity>

    @Query("""
        SELECT * FROM body_weight
        WHERE epoch_day BETWEEN :from AND :to
          AND ( epoch_day > :lastDay
                OR (epoch_day = :lastDay AND created_at > :lastCreatedAt)
                OR (epoch_day = :lastDay AND created_at = :lastCreatedAt AND id > :lastId) )
          AND ( epoch_day < :maxDay
                OR (epoch_day = :maxDay AND created_at < :maxCreatedAt)
                OR (epoch_day = :maxDay AND created_at = :maxCreatedAt AND id <= :maxId) )
        ORDER BY epoch_day ASC, created_at ASC, id ASC
        LIMIT :limit
    """)
    suspend fun weightPage(
        from: Long, to: Long,
        lastDay: Long, lastCreatedAt: Long, lastId: Long,
        maxDay: Long, maxCreatedAt: Long, maxId: Long,
        limit: Int,
    ): List<BodyWeightEntity>

    /** 出力開始時の上限キー。行がなければ null */
    @Query("""
        SELECT epoch_day, created_at, id FROM session
        WHERE epoch_day BETWEEN :from AND :to
        ORDER BY epoch_day DESC, created_at DESC, id DESC LIMIT 1
    """)
    suspend fun maxSessionKey(from: Long, to: Long): PageKey?

    @Query("""
        SELECT epoch_day, created_at, id FROM body_weight
        WHERE epoch_day BETWEEN :from AND :to
        ORDER BY epoch_day DESC, created_at DESC, id DESC LIMIT 1
    """)
    suspend fun maxWeightKey(from: Long, to: Long): PageKey?

    /** ページ 1 回分の取得をまとめる。トランザクションはこの取得で閉じる */
    @Transaction
    suspend fun strengthPage(
        from: Long, to: Long, last: PageKey, max: PageKey, limit: Int,
    ): CsvPage
}

data class PageKey(
    @ColumnInfo(name = "epoch_day") val epochDay: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val id: Long,
) {
    companion object { val START = PageKey(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE) }
}

data class CsvPage(
    val sessions: List<SessionEntity>,
    val records: List<ExerciseRecordEntity>,
    val sets: List<WorkoutSetEntity>,
)
```

- ページサイズは、セッション 50 件・体重 500 件とする（詳細確定）。1 ページの最大読み取り量は、想定規模（M-11）で種目記録 400 件・セット 2,000 件となる。
- 初回呼び出しの `last` には `PageKey.START` を渡す。
- 上限キー（`max`）は出力開始時に 1 度だけ取得し、全ページで同じ値を使う。これにより、出力中に追加された行は出力対象に入らない（本書 7.2）。
- ページングの順序は M-12 の並び順と一致する。
- `recordsForSessions` と `setsForRecords` に空リストを渡さない。呼び出し側で空判定を行う。

### 2.5 AppDatabase と初期化

```kotlin
@Database(
    entities = [ExerciseEntity::class, SessionEntity::class, ExerciseRecordEntity::class,
                WorkoutSetEntity::class, BodyWeightEntity::class,
                TemplateEntity::class, TemplateItemEntity::class, RestTimerEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun sessionDao(): SessionDao
    abstract fun recordRangeDao(): RecordRangeDao
    abstract fun exerciseRecordDao(): ExerciseRecordDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun bodyWeightDao(): BodyWeightDao
    abstract fun volumeDao(): VolumeDao
    abstract fun templateDao(): TemplateDao
    abstract fun templateItemDao(): TemplateItemDao
    abstract fun restTimerDao(): RestTimerDao
    abstract fun csvDao(): CsvDao
}
```

ビルダー設定（詳細確定）。

| 設定 | 値 | 根拠 |
|---|---|---|
| ジャーナルモード | 既定（WAL） | 基本設計 2.9 |
| `fallbackToDestructiveMigration` | 使用しない | 基本設計 2.9 |
| `addCallback` | `DatabaseCallback` | 索引作成と初期データ投入 |
| 外部キー | `onOpen` で `PRAGMA foreign_keys = ON` | 基本設計 2.5 |
| `exportSchema` | true。出力先 `app/schemas` をリポジトリに含める | 基本設計 2.9 |

### 2.6 条件付き一意索引の作成と維持

Room の `@Index` は条件付き索引を表現できず、スキーマ JSON にも現れない。索引の定義を 1 か所に集約し、作成経路をすべてそこへ通す（詳細確定。レビュー指摘に対応）。

```kotlin
object IndexDefinitions {
    const val EXERCISE_NAME_KEY_ACTIVE = "idx_exercise_name_key_active"

    val ALL: List<String> = listOf(
        "DROP INDEX IF EXISTS $EXERCISE_NAME_KEY_ACTIVE",
        "CREATE UNIQUE INDEX $EXERCISE_NAME_KEY_ACTIVE " +
            "ON exercise(name_key) WHERE is_deleted = 0",
    )
}
```

作成・再作成の規約。

1. `DatabaseCallback#onCreate` で `IndexDefinitions.ALL` を順に実行する。
2. **すべての `Migration` の末尾で `IndexDefinitions.ALL` を再実行する**。索引はスキーマ JSON の差分に現れないため、移行で失われても Room の検証では検出されない。`DROP INDEX IF EXISTS` を先に実行するのは、既存の索引が残っている場合に `CREATE` が失敗しないようにするためである。
3. 索引の存在と定義を、インメモリ DB を用いたスキーマテストで検証する（本書 12.2）。検証は次の 2 段階で行う。

```sql
-- 索引が存在し、定義に WHERE 句を含むこと
SELECT sql FROM sqlite_master
WHERE type = 'index' AND name = 'idx_exercise_name_key_active';

-- 一意であること
PRAGMA index_list('exercise');
```

4. 加えて、振る舞いのテストとして「同名の有効種目を 2 件登録すると失敗する」「削除済みと同名の種目は登録できる」を確認する（基本設計 2.4.1）。
5. 将来の `Migration` を追加する際は、上記 2 と 3 をテンプレートとして必ず含める。テストがない `Migration` はレビューで差し戻す。

### 2.7 初期データ投入

`DatabaseCallback#onCreate` の処理順（詳細確定）。

1. `IndexDefinitions.ALL` を実行する。
2. 付録 A の 53 件を、記載順（胸 9 → 背中 10 → 脚 10 → 肩 8 → 腕 10 → 体幹 6）で挿入する。`id` は 1 から 53 の昇順になる（基本設計 2.8）。
3. `name_key` は投入時に `NameKey.of(name)`（本書 4.5）で生成する。
4. `created_at` と `updated_at` には投入時刻を入れる。全件同一値でよい。
5. `rest_timer` には行を作らない。停止中は行がない状態で表す（基本設計 5.1）。

```kotlin
data class InitialExercise(val name: String, val bodyPart: BodyPart, val isBodyweight: Boolean)
```

- 区分は全件 `STRENGTH` とする（要件 3.5）。
- 自重フラグを立てるのは、ディップス、チンニング、バックエクステンション、クランチ、レッグレイズ、ハンギングレッグレイズ、アブローラーの 7 件（要件 8.1）。
- 投入は `onCreate` のコールバック内で行う。Room の `onCreate` は DB のオープンを完了させる前に呼ばれるため、DAO を経由せず生 SQL の `INSERT` で行う（詳細確定）。

### 2.8 DataStore

```kotlin
object SettingsKeys {
    val REST_TARGET_SEC = intPreferencesKey("rest_target_sec")
    val NOTIFICATION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    val LAST_ALARM_TOKEN = longPreferencesKey("last_alarm_token")
    val LAST_ALARM_TARGET_SET_ID = longPreferencesKey("last_alarm_target_set_id")
}
```

| キー | 型 | 既定値 | 用途 |
|---|---|---|---|
| `rest_target_sec` | Int | 90（M-7） | 休憩タイマーの目安秒数（要件 4.4.4） |
| `notification_permission_requested` | Boolean | false | `POST_NOTIFICATIONS` の要求を一度でも出したか（本書 5.10） |
| `last_alarm_token` | Long | なし | 最後に登録したアラームの世代。取り残された予約の解除に用いる（本書 5.3） |
| `last_alarm_target_set_id` | Long | なし | 同上 |

- `rest_target_sec` の読み出し時に範囲外（1〜3600 の外）を検出した場合は既定値へ丸める。
- `last_alarm_*` は `AlarmSchedulerImpl` だけが読み書きする。登録の直前に書き、解除の直後に消す（本書 5.4）。
- `SettingsDataSource` は `Flow<AppSettings>` を公開する。DataStore の生の `Preferences` を上位へ渡さない。

```kotlin
data class AppSettings(
    val restTargetSec: Int,
    val notificationRequested: Boolean,
)
```

### 2.9 order_index の維持

基本設計 2.6 の規則を、次の手順で実装する（詳細確定）。

```
reindex(parentId):
    ids = orderedIds(parentId)              -- order_index ASC, id ASC
    ids.forEachIndexed { index, id -> updateOrderIndex(id, index) }

move(parentId, targetId, direction):
    ids = orderedIds(parentId)
    i = ids.indexOf(targetId)
    if (i < 0) return AppError.TargetGone
    j = if (direction == UP) i - 1 else i + 1
    if (j !in ids.indices) return Success       -- 端では何もしない
    ids.swap(i, j)
    ids.forEachIndexed { index, id -> updateOrderIndex(id, index) }
```

- 全体を呼び出し元のトランザクション内で実行する。`order_index` に一意制約を置かないため、逐次更新の途中状態が他の読み取りへ観測されることはない。
- 基本設計 2.6 が求める「一括更新」の意図は「途中状態で並びが壊れないこと」である。単一トランザクション内の逐次更新はこれを満たす。相関副問い合わせによる 1 文の `UPDATE` は、更新中の同一テーブルを参照するため SQLite の評価順に依存する。採用しない。
- 対象行数はセット 20 件程度、種目記録 8 件程度、テンプレート明細 20 件程度であり、逐次更新の回数は問題にならない。

---

## 3. Repository 詳細設計

### 3.1 一覧と共通規約

| Repository | 責務 |
|---|---|
| `ExerciseRepository` | 種目マスタの参照・追加・編集・削除、テンプレートへの波及（基本設計 4.6） |
| `RecordRepository` | セッション・種目記録・セットの参照と確定・編集・削除、タイマーの引き継ぎ |
| `BodyWeightRepository` | 体重の参照・追加・編集・削除 |
| `TemplateRepository` | テンプレートの参照・作成・編集・削除・明細の取得 |
| `VolumeRepository` | 総ボリューム・部位別内訳・種目別推移の算出 |
| `RestTimerRepository` | `rest_timer` の参照と、完了・停止・リセット・破棄のトランザクション |
| `SettingsRepository` | DataStore の設定値 |

共通規約（詳細確定）。

- すべて `interface` と `Impl` に分け、Hilt でバインドする。ViewModel は `interface` にのみ依存する。
- 更新系は `AppResult<T>` を返す。例外は `Impl` の境界で捕捉し `AppError.Unexpected` へ変換する。`CancellationException` は再スローする。
- 参照系の `Flow` は `LoadResult<T>` を流す（本書 1.6）。例外で空リストを流さない。
- 更新系は例外なく 3.2 の保存前検証を通す。UI からの入力値を検証の根拠にしない。
- 更新結果が 0 件だった場合の扱いは 3.3 の表に従う。

### 3.2 保存前の不変条件検証

レビュー指摘に従い、Repository が UI を経由せずに呼ばれても不変条件を保てるようにする（詳細確定）。すべての更新系メソッドは、対象行と親行を**トランザクション内で再取得**し、次を確認してから書き込む。

```kotlin
internal object Invariants {
    /** 当日でなければ PastDayNotEditable。基本設計 4.1 */
    fun requireToday(epochDay: Long, today: LocalDate): AppError?

    /** 種目記録の区分が期待と異なれば CategoryMismatch */
    fun requireCategory(actual: ExerciseCategory, expected: ExerciseCategory): AppError?

    /** 自重フラグと重量 NULL の対応が崩れていれば WeightInconsistent */
    fun requireWeightConsistency(isBodyweight: Boolean, weightTenthKg: Int?): AppError?
}
```

| 検証 | 内容 | 適用先 |
|---|---|---|
| 行の存在 | 対象行と親行を再取得し、いずれかが NULL なら `TargetGone` | 全更新系 |
| 当日 | 親セッションの `epoch_day` が `TimeProvider.today().toEpochDay()` と一致すること | セット追加・編集、実施時間編集、セッション名編集、並び替え |
| 区分 | セット操作は `snapshot_category = strength`、実施時間操作は `cardio` であること | セット追加・編集、実施時間編集 |
| 重量の整合 | `snapshot_is_bodyweight = true` なら `weightTenthKg == null`、`false` なら非 NULL かつ 7.1 の範囲・刻みを満たすこと | セット追加・編集 |
| 種目の有効性 | 記録の確定時、`exercise` が存在し `is_deleted = 0` であること。そうでなければ `TargetGone` | セット確定、有酸素確定、テンプレート明細追加 |
| 値域 | 4.4 の `Validators` を通ること | 全更新系 |

- 削除系には当日検証を適用しない。要件 4.3.2 が当日・過去日を問わず削除を認めるためである。
- 休憩タイマーによる `rest_sec` の書き込みには当日検証を適用しない（M-16、基本設計 4.1）。
- 検証は ViewModel でも行う。入力欄へ即座にエラーを返すためである。Repository 側の検証はそれとは独立に必ず実行する。二重に検証することを冗長とみなさない。

### 3.3 対象 0 件時の扱い

更新・削除の戻り値が 0 件だった場合の判定を一覧で定める（詳細確定）。

| 操作 | 0 件の意味 | 返す `AppError` |
|---|---|---|
| `updateWeightAndReps` | セットが削除済み | `TargetGone` |
| `updateRestSec` | セットが削除済み。タイマー完了の経路では 3.12 の削除件数で判定する | 呼び出し元で分岐 |
| `updateDurationMin` | 種目記録が削除済み | `TargetGone` |
| `updateName`（セッション） | セッションが削除済み | `TargetGone` |
| `markDeleted`（種目） | 種目が存在しないか既に削除済み | `TargetGone` |
| `updateAttributes`（種目） | 種目が存在しない | `TargetGone` |
| `update`（体重） | 記録が削除済み | `TargetGone` |
| `updateName`（テンプレート） | テンプレートが削除済み | `TargetGone` |
| `deleteById`（各種） | 既に削除済み | 成功として扱う。削除の目的は達成されている |
| `deleteMatching`（タイマー） | 世代が入れ替わっている | `false` を返す。エラーにしない（基本設計 5.1.2） |

- 削除の 0 件を成功として扱うのは、削除操作が冪等であるべきだからである。利用者から見て「消えている」状態は達成されている。
- 更新の 0 件をエラーにするのは、利用者が編集した内容が失われたことを知らせる必要があるためである。

### 3.4 RecordRepository

```kotlin
interface RecordRepository {
    fun observeDay(epochDay: Long): Flow<LoadResult<DayRecords>>

    /** 下書きの最初のセットを確定する。種目記録を新規作成する */
    suspend fun commitFirstStrengthSet(
        target: SessionTarget,
        exerciseId: Long,
        weightTenthKg: Int?, reps: Int, memo: String?,
        targetSec: Int,
    ): AppResult<StrengthCommitResult>

    /** 2 セット目以降。既存の種目記録へ追加する */
    suspend fun appendStrengthSet(
        recordId: Long,
        weightTenthKg: Int?, reps: Int, memo: String?,
        targetSec: Int,
    ): AppResult<StrengthCommitResult>

    suspend fun commitCardioRecord(
        target: SessionTarget, exerciseId: Long, durationMin: Int,
    ): AppResult<CardioCommitResult>

    suspend fun updateSet(setId: Long, weightTenthKg: Int?, reps: Int): AppResult<Unit>
    suspend fun updateCardioDuration(recordId: Long, durationMin: Int): AppResult<Unit>
    suspend fun updateSessionName(sessionId: Long, name: String?): AppResult<Unit>

    suspend fun deleteSet(setId: Long): AppResult<TimerCancellation>
    suspend fun deleteRecord(recordId: Long): AppResult<TimerCancellation>
    suspend fun deleteSession(sessionId: Long): AppResult<TimerCancellation>

    suspend fun moveRecord(sessionId: Long, recordId: Long, direction: MoveDirection): AppResult<Unit>
    suspend fun moveSet(recordId: Long, setId: Long, direction: MoveDirection): AppResult<Unit>

    suspend fun createSession(epochDay: Long, name: String?): AppResult<Long>
}

data class StrengthCommitResult(
    val sessionId: Long,
    val recordId: Long,          // 呼び出し元が下書きへ保存し、次のセットで appendStrengthSet に渡す
    val setId: Long,
    val handover: TimerHandover,
)

data class CardioCommitResult(val sessionId: Long, val recordId: Long)

data class TimerHandover(
    val cancel: TimerCancellation,        // 旧世代の解除指示
    val newToken: Long,
    val newTargetSetId: Long,
    val fireAtElapsedMillis: Long,
)
```

レビュー指摘に対応し、`draftId` を引数から削除した（詳細確定）。Repository は ViewModel の下書き対応表を参照できないため、`draftId` から既存の `recordId` を解決する契約は実装できない。代わりに次の規則とする。

- 下書きの **1 セット目**は `commitFirstStrengthSet` を呼ぶ。戻り値の `recordId` を下書きへ保存する。
- **2 セット目以降**は必ず `appendStrengthSet(recordId, ...)` を呼ぶ。Repository は `recordId` の存在と区分をトランザクション内で確認する。
- 同一種目を同一セッションに複数の種目記録として持つことは要件 4.6 が認めている。したがって「セッション ID と種目 ID から既存の種目記録を探す」処理は行わない。探索すると、意図せず別の下書きの種目記録へ追記される。
- 下書きの状態遷移は 6.3.1 の表に定める。

### 3.5 commitFirstStrengthSet のトランザクション

基本設計 12 章「筋トレの初回セット確定」に対応する。単一の `@Transaction` 内で以下を順に行う。

1. `today = TimeProvider.today().toEpochDay()` を求める。
2. セッションを解決する。
   - `Existing(id)`: `sessionDao.findById(id)` を取得する。NULL なら `TargetGone`。`epoch_day != today` なら `PastDayNotEditable`（Q-4）。
   - `AutoCreateOnCommit`: `sessionDao.findByDay(today)` を取得する。0 件なら無名セッションを 1 件作成する。1 件以上あれば `created_at, id` の昇順で最後のセッションを使う（基本設計 3.2.1、Q-7）。
3. 種目を解決する。`exerciseDao.findById(exerciseId)` が NULL、または `isDeleted` なら `TargetGone`。取得した行から `ExerciseSnapshot` を組み立てる。UI から渡されたスナップショットは使わない（レビュー指摘に対応）。
4. 区分を確認する。`snapshot.category != STRENGTH` なら `CategoryMismatch`。
5. 重量の整合を確認する。`snapshot.isBodyweight` が真なら `weightTenthKg == null`、偽なら非 NULL かつ 7.1 の範囲・刻みを満たすこと。崩れていれば `WeightInconsistent`。
6. `nextOrderIndex(sessionId)` を採番し、`exercise_record` を挿入する。`duration_min` は NULL、スナップショット 4 列に手順 3 の値を書く。
7. `nextOrderIndex(recordId)` を採番し、`workout_set` を挿入する。`rest_sec` は NULL。
8. 旧タイマーを確定する。`restTimerDao.get()` を読み、行があれば
   - `recordedRestSec`（本書 5.2）を `updateRestSec(oldTargetSetId, restSec)` で書き込む。
   - `deleteMatching(oldToken, oldTargetSetId)` で削除する。削除件数が 0 なら例外を投げてトランザクションを中断する（3.12 と同じ理由）。
   - `TimerCancellation.Cancel(oldToken, oldTargetSetId)` を控える。行がなければ `TimerCancellation.None`。
9. 新しい `token` を採番し、`rest_timer` を挿入する。`target_set_id` は手順 7 のセット ID、`started_elapsed_millis` と `started_boot_count` と `started_at_millis` は `TimeProvider` の現在値、`target_sec` は引数の値。
10. `TimerHandover` を組み立てて返す。`fireAtElapsedMillis = 現在の elapsedRealtime + targetSec * 1000`。

トランザクションの外では、`RestTimerCoordinator.applyHandover` が旧世代のアラームを解除し、新世代を登録する（本書 5.5）。

- `appendStrengthSet` は手順 2 と 6 を「引数の `recordId` から `findContextByRecordId` を取得し、`epoch_day` が当日、`snapshot_category` が `strength` であることを確認する」に置き換える。手順 3 は行わない。既存の種目記録のスナップショットをそのまま使う。手順 5 の自重判定には `RecordContext.snapshotIsBodyweight` を用いる。
- `commitCardioRecord` は手順 1 から 6 までを行い、手順 6 で `duration_min` に入力値を書く。手順 4 の期待値は `CARDIO`。手順 7 以降は行わない。有酸素はセットとタイマーを持たない（要件 3.1、基本設計 5.7）。

### 3.6 削除系のトランザクション

| メソッド | トランザクション内 | 戻り値 |
|---|---|---|
| `deleteSet` | `restTimerDao.generationForSet(setId)` で世代を控える → `workoutSetDao.deleteById` → 同一種目記録内の `reindex` | 控えた世代（なければ `None`） |
| `deleteRecord` | `restTimerDao.generationForRecord(recordId)` で世代を控える → `exerciseRecordDao.deleteById`（CASCADE で配下セットとタイマー行が消える）→ セッション内の `reindex` | 同上 |
| `deleteSession` | `restTimerDao.generationForSession(sessionId)` で世代を控える → `sessionDao.deleteById`（CASCADE） | 同上 |

- 世代の取得を削除より先に行う。削除後は CASCADE でタイマー行が消え、解除すべき世代が分からなくなる（基本設計 5.5.3）。
- トランザクションの外で `RestTimerCoordinator.cancelAlarmIfNeeded(cancellation)` を呼ぶ。`None` のときは何もしない。
- 削除に当日検証を適用しない（要件 4.3.2）。

### 3.7 ExerciseRepository

```kotlin
interface ExerciseRepository {
    fun observeActive(): Flow<LoadResult<List<Exercise>>>
    fun observeActiveWithUsage(): Flow<LoadResult<List<ExerciseWithUsageCount>>>
    fun observeGraphSelectable(): Flow<LoadResult<List<Exercise>>>
    suspend fun findById(id: Long): AppResult<Exercise>

    suspend fun addExercise(input: ExerciseInput): AppResult<Long>
    suspend fun updateExercise(id: Long, input: ExerciseInput): AppResult<ExerciseUpdateEffect>
    suspend fun deleteExercise(id: Long): AppResult<Unit>
}

/** 区分変更でテンプレートから取り除かれたかを UI へ伝える */
data class ExerciseUpdateEffect(val removedFromTemplateCount: Int)
```

`addExercise` の `@Transaction`。

1. `Validators` で入力を検証する（本書 4.4）。
2. Q-3 の対応を確認する。`category == CARDIO` なら `bodyPart == CARDIO` かつ `isBodyweight == false`、`category == STRENGTH` なら `bodyPart != CARDIO` であること。崩れていれば `CategoryMismatch`。
3. `NameKey.of(input.name)` を生成し、`findActiveByNameKey` で重複を確認する。あれば `DuplicateExerciseName`。
4. `insert` する。`SQLiteConstraintException` を捕捉して `DuplicateExerciseName` へ変換する（条件付き一意索引による防御）。

`updateExercise` の `@Transaction`。

1. 手順 1 から 3 は `addExercise` と同じ。ただし手順 3 の重複確認では自分自身の ID を除外する。
2. `findById(id)` が NULL なら `TargetGone`。現在の `category` を控える。
3. `updateAttributes` を実行する。戻り値 0 なら `TargetGone`。
4. 区分が変わった場合のみ、`affectedTemplateIds(id)` を控え、`deleteByExercise(id)` を実行し、控えた各テンプレートで `reindex` を行う（基本設計 4.6）。
5. 部位・種目名・自重フラグのみの変更ではテンプレートに触れない（要件 3.4）。
6. 過去の記録行には触れない（要件 3.4）。

`deleteExercise` の `@Transaction`。

1. `markDeleted(id, now)` を実行する。戻り値 0 なら `TargetGone`。
2. `affectedTemplateIds(id)` を控える。
3. `deleteByExercise(id)` を実行する。
4. 控えた各テンプレートで `reindex` を行う。

- 手順 2 と 3 の順序を守る。先に削除すると対象テンプレートが分からなくなる。

### 3.8 BodyWeightRepository

```kotlin
interface BodyWeightRepository {
    fun observeByRange(from: Long, to: Long): Flow<LoadResult<List<BodyWeightRecord>>>
    fun observeDailyAverages(from: Long, to: Long): Flow<LoadResult<List<DailyBodyWeight>>>

    suspend fun add(epochDay: Long, weightTenthKg: Int): AppResult<Long>
    suspend fun updateWeight(id: Long, weightTenthKg: Int): AppResult<Unit>
    suspend fun delete(id: Long): AppResult<Unit>
}

data class BodyWeightRecord(val id: Long, val epochDay: Long, val weightTenthKg: Int, val createdAt: Long)
data class DailyBodyWeight(val epochDay: Long, val averageTenthKg: Int)
```

| メソッド | 検証 | トランザクション |
|---|---|---|
| `add` | 体重が 1〜9999（0.1 kg 単位）。日付は当日・過去日いずれも可（要件 3.8）。未来日は不可 | 単一の `insert` |
| `updateWeight` | `findById` が NULL なら `TargetGone`。`epoch_day` が当日でなければ `PastDayNotEditable`（Q-5）。値域を検証 | 取得と更新を同一トランザクション |
| `delete` | 日付を問わず可能（要件 3.8）。0 件は成功 | 単一の `deleteById` |

- 日付の変更は行えない。`updateWeight` に日付の引数を持たせない（16 章 D-3）。
- 未来日の追加を拒否するのは、要件 3.8 が「過去日の体重を追加できる」とのみ定めており、未来日を認めていないためである（詳細確定）。
- `observeDailyAverages` は `observeByRange` の結果から日別平均（M-14）を算出して返す。SQL の `AVG` を用いない。整数演算と丸め規則を Kotlin 側に揃えるためである。

### 3.9 TemplateRepository

```kotlin
interface TemplateRepository {
    fun observeTemplates(): Flow<LoadResult<List<TemplateWithItems>>>
    suspend fun findWithItems(templateId: Long): AppResult<TemplateWithItems>

    suspend fun createTemplate(name: String, items: List<TemplateItemInput>): AppResult<Long>
    suspend fun updateTemplate(id: Long, name: String, items: List<TemplateItemInput>): AppResult<Unit>
    suspend fun deleteTemplate(id: Long): AppResult<Unit>
}

data class TemplateItemInput(val exerciseId: Long, val setCount: Int?)

data class TemplateWithItems(
    val id: Long, val name: String,
    val items: List<TemplateItemDetail>,
)

data class TemplateItemDetail(
    val itemId: Long, val exerciseId: Long, val exerciseName: String,
    val bodyPart: BodyPart, val category: ExerciseCategory,
    val isBodyweight: Boolean, val orderIndex: Int, val setCount: Int?,
)
```

`createTemplate` / `updateTemplate` の `@Transaction`。

1. 名称を検証する（必須、50 コードポイント、空白のみ不可）。重複は許す（Q-13）。
2. 明細を検証する。
   - 新規作成では明細 0 件を許可しない（Q-12）。`ValidationKind.Required` を返す。
   - 更新では 0 件を許可する。種目の削除・区分変更で 0 件になった場合と扱いを揃える（M-18）。
   - 各明細の `exerciseId` について、`exercise` が存在し `is_deleted = 0` であることを確認する。そうでなければ `TargetGone`。
   - 種目の区分が `STRENGTH` なら `setCount` が 1〜20（Q-12）、`CARDIO` なら `setCount == null`（要件 4.6）。崩れていれば `Validation` または `CategoryMismatch`。
3. 更新の場合、`deleteByTemplate(id)` で既存明細を全削除してから入れ直す。差分更新を行わない。並びと件数の整合を保つ処理を単純にするためである（詳細確定）。
4. `order_index` は引数リストの並び順で 0 起点の連番を振る。
5. `updated_at` を現在時刻で更新する。

### 3.10 VolumeRepository

```kotlin
interface VolumeRepository {
    fun observeDayVolume(epochDay: Long): Flow<LoadResult<Long>>                    // 0.1 kg 単位
    fun observePeriodSummary(range: DateRange): Flow<LoadResult<PeriodSummary>>
    fun observeExerciseSeries(exerciseId: Long, range: DateRange): Flow<LoadResult<List<VolumePoint>>>
}

data class PeriodSummary(val totalTenth: Long, val byPart: Map<BodyPart, Long>)
data class VolumePoint(val epochDay: Long, val volumeTenth: Long)
```

レビュー指摘に対応し、購読の構成を明示する（詳細確定）。

```kotlin
override fun observePeriodSummary(range: DateRange): Flow<LoadResult<PeriodSummary>> =
    combine(
        volumeDao.observeDailyPartVolumeNonBodyweight(range.fromEpochDay, range.toEpochDay),
        volumeDao.observeDailyPartRepsBodyweight(range.fromEpochDay, range.toEpochDay),
        bodyWeightDao.observeRevision(),          // 体重テーブルの変更信号
    ) { nonBw, bwReps, _ -> Triple(nonBw, bwReps, Unit) }
        .mapLatest { (nonBw, bwReps, _) ->
            val resolver = buildResolver(range, needed = bwReps.isNotEmpty())
            LoadResult.Ready(summarize(nonBw, bwReps, resolver))
        }
        .catch { emit(LoadResult.Failed(it.toAppError(), lastValue = null)) }
        .flowOn(dispatchers.default)
```

- 集計の 2 本は `Flow` であり、`session` / `exercise_record` / `workout_set` の変更で再発行される。
- 体重の変更は上記 3 テーブルに現れないため、`observeRevision()` を第 3 の入力として `combine` する。これにより、体重の追加・編集・削除でも自重種目の総ボリュームが再計算される（要件 4.8）。
- `buildResolver` は `mapLatest` の中で `suspend` として実行する。自重の行が 1 件もない場合は体重を取得しない。
- `mapLatest` を用いることで、連続した変更では最新の 1 回だけが完了する。

`buildResolver` の手順（基本設計 4.3.1）。

1. `bodyWeightDao.dailyTotals(from, to)` で期間内の日別合計と件数を取得する。
2. `bodyWeightDao.latestDayBefore(from)` で期間開始前の直近日を求める。
3. 手順 2 が非 NULL なら `bodyWeightDao.totalForDay(day)` でその日の全記録を取得する。
4. 各日の平均を `dailyAverageTenth`（本書 4.3）で求め、`BodyWeightResolver` を組み立てる。

`observeExerciseSeries` も同じ構成とする。`observeExerciseDaily` の `is_bodyweight = 1` の行に解決体重を掛け、同一日の 2 行を合算して 1 点にする（要件 4.9.1）。

`observeDayVolume` は `observePeriodSummary(DateRange(day, day))` の `totalTenth` を返す。別のクエリを持たない。

### 3.11 SettingsRepository

```kotlin
interface SettingsRepository {
    fun observe(): Flow<AppSettings>
    suspend fun setRestTargetSec(sec: Int): AppResult<Unit>
    suspend fun markNotificationRequested(): AppResult<Unit>
}
```

- `setRestTargetSec` は 1〜3600 を検証する（要件 3.9）。範囲外は `Validation(REST_TARGET_SEC, OutOfRange)`。
- 計測中に変更しても進行中のタイマーには適用しない。`rest_timer.target_sec` が開始時点の値を保持しているため（基本設計 5.9）。
- `last_alarm_*` は本 Repository の対象外とする。`AlarmSchedulerImpl` が直接 `SettingsDataSource` を使う（本書 5.4）。

### 3.12 RestTimerRepository

```kotlin
interface RestTimerRepository {
    fun observe(): Flow<RestTimerState?>
    suspend fun get(): RestTimerState?

    /** 到達による完了。rest_sec を書いて削除する。削除できたら true（基本設計 5.1.2） */
    suspend fun completeMatching(token: Long, targetSetId: Long, restSec: Int): Boolean

    /** 手動停止。completeMatching と同じ処理 */
    suspend fun stopMatching(token: Long, targetSetId: Long, restSec: Int): Boolean

    /** 手動リセット。rest_sec を書かずに削除する */
    suspend fun resetMatching(token: Long, targetSetId: Long): Boolean

    /** 端末再起動による破棄。rest_sec を書かずに削除する */
    suspend fun discardMatching(token: Long, targetSetId: Long): Boolean
}
```

レビュー指摘に対応し、ロールバックの実装を分離する（詳細確定）。`@Transaction` メソッドの内部で例外を捕捉するとロールバックされないため、**例外を投げる内部メソッド**と、**その外側で例外を `Boolean` へ変換するラッパー**に分ける。

```kotlin
internal class RestTimerLostGenerationException : Exception()

@Dao
abstract class RestTimerTxDao {
    @Transaction
    open suspend fun completeOrThrow(token: Long, targetSetId: Long, restSec: Int) {
        val row = get() ?: throw RestTimerLostGenerationException()
        if (row.token != token || row.targetSetId != targetSetId) {
            throw RestTimerLostGenerationException()
        }
        updateRestSec(targetSetId, restSec)
        val deleted = deleteMatching(token, targetSetId)
        if (deleted != 1) throw RestTimerLostGenerationException()   // ここでロールバックされる
    }
}

// Repository 実装側（トランザクションの外）
override suspend fun completeMatching(token: Long, targetSetId: Long, restSec: Int): Boolean =
    try {
        txDao.completeOrThrow(token, targetSetId, restSec)
        true
    } catch (e: RestTimerLostGenerationException) {
        false
    }
```

- 例外は `@Transaction` メソッドの**外**へ抜ける必要がある。抜けて初めて Room がロールバックする。内部で `try/catch` すると、`updateRestSec` の書き込みだけがコミットされ、別世代のタイマーの休憩時間を上書きしうる。
- `RestTimerLostGenerationException` は正常系の分岐であり、`AppError.Unexpected` へ変換しない。ログにも出さない。
- `stopMatching` は `completeMatching` と同一の処理を呼ぶ。呼び出し元の意味づけが異なるだけである。
- `resetMatching` と `discardMatching` は `updateRestSec` を行わない。`rest_sec` は NULL のまま残る（要件 4.4.1、4.4.2）。

---

## 4. ドメイン層詳細設計

### 4.1 ドメインモデル

```kotlin
data class DayRecords(
    val epochDay: Long,
    val sessions: List<SessionRecords>,
)

data class SessionRecords(
    val sessionId: Long,
    val name: String?,
    val displayIndex: Int,                 // M-17 の連番。1 起点
    val records: List<ExerciseRecordItem>,
)

data class ExerciseRecordItem(
    val recordId: Long,
    val exerciseId: Long,
    val snapshot: ExerciseSnapshot,
    val orderIndex: Int,
    val durationMin: Int?,                 // 有酸素のみ非 NULL
    val sets: List<WorkoutSetItem>,        // 有酸素は常に空
)

data class WorkoutSetItem(
    val setId: Long, val orderIndex: Int,
    val weightTenthKg: Int?, val reps: Int,
    val restSec: Int?, val memo: String?,
)

data class Exercise(
    val id: Long, val name: String, val bodyPart: BodyPart,
    val category: ExerciseCategory, val isBodyweight: Boolean, val isDeleted: Boolean,
)

data class ExerciseWithUsageCount(val exercise: Exercise, val usageCount: Long)

data class RestTimerState(
    val token: Long, val targetSetId: Long, val targetSec: Int,
    val startedElapsedMillis: Long, val startedBootCount: Int, val startedAtMillis: Long,
)

data class DateRange(val fromEpochDay: Long, val toEpochDay: Long)   // 両端を含む
```

- `displayIndex` は同一日のセッションを `created_at, id` の昇順に並べたときの位置（1 起点）である。DB には保存しない。表示のたびに算出する（M-17）。

### 4.2 総ボリュームの算出

基本設計 4.3 と 1.5 に従う純粋関数とする。

```kotlin
object VolumeCalculator {
    fun setVolumeTenth(weightTenthKg: Int, reps: Int): Long = weightTenthKg.toLong() * reps
    fun bodyweightSetVolumeTenth(bodyWeightTenthKg: Int, reps: Int): Long = bodyWeightTenthKg.toLong() * reps
    fun bodyweightRepsVolumeTenth(repsTotal: Long, bodyWeightTenthKg: Int): Long = repsTotal * bodyWeightTenthKg
}
```

- 乗算は必ず左辺を `Long` に昇格させる（基本設計 1.5）。`Int` 同士で掛けてから `Long` へ代入する記述を禁止する。静的解析では検出できないため、コードレビューの確認項目とする（詳細確定）。
- 有酸素は本計算器を通さない。呼び出し側で `snapshot_category` により除外する。

### 4.3 体重の解決

基本設計 4.3.1 の 3 段階をそのまま実装する。

```kotlin
class BodyWeightResolver(
    private val days: LongArray,          // 昇順。dailyAverages のキー
    private val averages: IntArray,       // days と同じ添字。0.1 kg 単位
    private val carryInDay: Long?,
    private val carryInAverage: Int?,
) {
    /** 解決できなければ null。呼び出し側は総ボリューム 0 として扱う */
    fun resolve(epochDay: Long): Int?
}

fun dailyAverageTenth(sumTenth: Long, count: Int): Int =
    BigDecimal(sumTenth)
        .divide(BigDecimal(count), 0, RoundingMode.HALF_UP)
        .toInt()
```

解決手順。

1. `days` を二分探索し、`epochDay` と一致する要素があれば対応する平均を返す。
2. なければ、`epochDay` より小さい最大の要素を求め、対応する平均を返す。
3. それもなければ、`carryInDay != null && carryInDay < epochDay` のとき `carryInAverage` を返す。
4. いずれにも当たらなければ `null` を返す。

- 対象日より後の記録は参照しない。手順 2 と 3 の比較は必ず「より小さい」で行う。
- 日ごとに DB へ問い合わせない。二分探索で解決する（基本設計 4.3.1）。
- 平均は丸めてから乗算する。平均に小数を持ち込まない（M-14）。
- 平均は必ずその日の全記録に対して求める。1 件だけを取得する経路を作らない。

### 4.4 入力検証

要件 3.9 と基本設計 7.1 をそのまま実装する。すべて純粋関数とし、ViewModel と Repository の双方から呼ぶ。

| 対象 | 検証内容 | `FieldId` |
|---|---|---|
| 重量 | 5 ≦ `tenth` ≦ 9995、かつ `tenth % 5 == 0` | `WEIGHT` |
| 回数 | 1 ≦ `reps` ≦ 999 | `REPS` |
| 有酸素の実施時間 | 1 ≦ `min` ≦ 999 | `DURATION_MIN` |
| 体重 | 1 ≦ `tenth` ≦ 9999 | `BODY_WEIGHT` |
| 目安秒数 | 1 ≦ `sec` ≦ 3600 | `REST_TARGET_SEC` |
| テンプレートのセット数 | 1 ≦ `count` ≦ 20（Q-12） | `TEMPLATE_SET_COUNT` |
| 種目名 | 必須、コードポイント数 ≦ 50、空白のみ不可 | `EXERCISE_NAME` |
| セッション名 | 任意、コードポイント数 ≦ 50、空白のみは NULL 保存 | `SESSION_NAME` |
| テンプレート名 | 必須、コードポイント数 ≦ 50、空白のみ不可 | `TEMPLATE_NAME` |
| セットのメモ | 任意、コードポイント数 ≦ 500、空白のみ可、前後空白を除去しない | `MEMO` |
| CSV の期間 | 開始日 ≦ 終了日 | `CSV_FROM_DATE` |

数値入力文字列の正規化（基本設計 7.1）。

```kotlin
fun normalizeNumberInput(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFKC).trim()

fun lengthInCodePoints(s: String): Int = s.codePointCount(0, s.length)
```

- NFKC により全角数字・全角小数点が半角へ変換される。個別の文字置換表は持たない。
- 重量と体重の文字列は `BigDecimal` で受け、`movePointRight(1)` の結果が整数でなければ `BadStep` とする。浮動小数点で解析しない。
- 文字数は `String.length`（UTF-16 コードユニット数）ではなくコードポイント数で数える（基本設計 7.2）。

### 4.5 name_key の生成

基本設計 7.2 の 4 段階を実装する。ただし順序を変更する（改訂提案 R-3）。

```kotlin
object NameKey {
    /** NFKC 後に残りうる空白。NFKC は Zs を U+0020 へ写すため、これで足りる */
    private val WHITESPACE = Regex("[\\u0009-\\u000D\\u0020\\u0085\\u2028\\u2029]+")

    fun of(raw: String): String =
        Normalizer.normalize(raw, Normalizer.Form.NFKC)
            .replace(WHITESPACE, " ")
            .trim()
            .lowercase(Locale.ROOT)

    fun displayName(raw: String): String = raw.trim()
}
```

- 基本設計 7.2 は「前後空白の除去 → NFKC → 連続空白の統合 → 小文字化」の順を定めるが、この順では NFKC 後に空白へ変わる文字が端に残る。全角スペース（U+3000）や NBSP（U+00A0）は NFKC で U+0020 へ写るため、先に `trim()` しても除去されない。本書は **NFKC → 空白統合 → trim → 小文字化** の順とする。
- 対象とする空白は上記の文字集合に限る。Java の `\s`（ASCII のみ）でも `\p{Zs}`（Unicode）でもなく、明示した集合を使う。NFKC 後は Zs が U+0020 に正規化されているため、行区切り（U+2028、U+2029）と制御系を加えれば十分である。
- `lowercase` には `Locale.ROOT` を渡す。端末ロケールに依存する変換（トルコ語の I など）を避ける。
- 表示用の `name` には `displayName(raw)` を保存する。NFKC も小文字化も適用しない（基本設計 7.2）。
- テンプレート名には `name_key` を持たない。重複を許すため（Q-13）。

### 4.6 期間の算出

```kotlin
enum class GraphPeriod { MONTH_1, MONTH_3, YEAR_1, ALL }
enum class SummaryUnit { WEEK, MONTH }
```

| 対象 | 実装 |
|---|---|
| グラフ 1 ヶ月 | `DateRange(today.minusMonths(1).plusDays(1), today)` |
| グラフ 3 ヶ月 | `DateRange(today.minusMonths(3).plusDays(1), today)` |
| グラフ 1 年 | `DateRange(today.minusYears(1).plusDays(1), today)` |
| グラフ 全期間 | `DateRange(exerciseOldestDay ?: today, today)` |
| 週サマリー | `date.with(previousOrSame(SUNDAY))` から `+6` 日 |
| 月サマリー | `YearMonth.from(date).atDay(1)` から `atEndOfMonth()` |

- `minusMonths` の月末丸めをそのまま採用する（基本設計 4.2）。3 月 31 日の 1 ヶ月前は 2 月 28 日または 29 日となる。
- 週の始まりは日曜日（要件 4.10）。
- 前方向への移動可能な下限は `RecordRangeDao.summaryOldestDay()` を含む期間とする。`null`（記録 0 件）の場合は前方向への移動を無効化する（16 章 D-8）。
- 後方向の上限は「今日を含む期間」とする。未来の期間へは移動しない（基本設計 4.2）。

### 4.7 日付変更の検知

レビュー指摘に対応し、画面を開いたままの日付跨ぎを検知する仕組みを設ける（詳細確定）。

```kotlin
class DateChangeNotifier @Inject constructor(
    private val time: TimeProvider,
    private val dispatchers: DispatcherProvider,
) {
    /** 現在の当日を流し、日付が変わるたびに新しい値を流す */
    fun observeToday(): Flow<LocalDate>
}
```

- 実装は、現在時刻から翌日 0 時までの残りミリ秒を求めて `delay` し、経過後に新しい `today()` を発行することを繰り返す。1 秒ごとのポーリングは行わない。
- 購読は画面が表示されている間だけ行う。`collectAsStateWithLifecycle` を用いる。
- S-01 は `observeToday()` を購読し、値が変わったら `epochDay` を新しい当日へ切り替えて再読み込みする。表示日が古いまま残らない。
- あわせて、保存操作の直前にも当日を再判定する（基本設計 4.1）。日付変更の検知と保存前の再判定は、どちらか一方では足りない。前者は表示の追従、後者は書き込みの防止を担う。

### 4.8 ユースケース一覧

Repository を 1 つ呼ぶだけの処理はユースケースを作らず、ViewModel から Repository を呼ぶ（詳細確定）。

| ユースケース | 責務 | 依存 |
|---|---|---|
| `CommitStrengthSetUseCase` | 検証 → `commitFirstStrengthSet` または `appendStrengthSet` → `RestTimerCoordinator.applyHandover` → 通知権限要求の要否判定 | Record, Settings, Timer |
| `CommitCardioRecordUseCase` | 検証 → `commitCardioRecord` | Record |
| `ApplyTemplateUseCase` | テンプレート明細から下書きリストを生成する。DB へ書かない（基本設計 3.3） | Template |
| `ObserveDayScreenUseCase` | `observeDay` と `observeDayVolume` と `RestTimerRepository.observe()` と `observeToday()` を `combine` する | Record, Volume, Timer, Date |
| `ObserveGraphUseCase` | 期間算出 → `observeExerciseSeries` → 軸情報の算出（M-2） | Volume, RecordRange |
| `ObserveSummaryUseCase` | 期間算出 → `observePeriodSummary` → 移動可能範囲の算出 | Volume, RecordRange |
| `ExportCsvUseCase` | 3 ファイルの出力制御。進捗と結果を `Flow` で返す | Csv |
| `RestoreRestTimerUseCase` | 基本設計 5.6 の復元・再照合 | Timer |

---

## 5. 休憩タイマー詳細設計

### 5.1 構成要素

| クラス | 層 | 責務 |
|---|---|---|
| `RestTimerCoordinator` | domain/timer | 完了・引き継ぎ・停止・リセット・復元の調停。DB 操作とアラーム・通知の順序を守る |
| `AlarmScheduler` | domain/timer（interface） | 世代指定でのアラーム登録・解除 |
| `RestNotifier` | domain/timer（interface） | 終了通知の表示 |
| `AlarmSchedulerImpl` | data/alarm | `AlarmManager` 操作と最終世代の記録 |
| `RestTimerAlarmReceiver` | data/alarm | `BroadcastReceiver`。`goAsync()` で Coordinator を呼ぶ |
| `RestNotifierImpl` | notification | 通知チャンネルと通知の組み立て |
| `RestTimerTicker` | ui | 1 秒周期の Flow。表示更新と到達検知（基本設計 5.1.3） |

- `RestTimerCoordinator` は Android の型に依存しない。`AlarmScheduler` と `RestNotifier` を `interface` として受け取る（基本設計 1.2）。

### 5.2 recordedRestSec

基本設計 5.1.1 の式をそのまま実装する。休憩時間を書き込むすべての経路がこの 1 関数を通る。

```kotlin
object RestSecCalculator {
    fun elapsedSec(nowElapsedMillis: Long, startedElapsedMillis: Long): Int =
        ((nowElapsedMillis - startedElapsedMillis) / 1000L)
            .coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()

    fun recordedRestSec(nowElapsedMillis: Long, state: RestTimerState): Int =
        minOf(elapsedSec(nowElapsedMillis, state.startedElapsedMillis), state.targetSec)
}
```

- 除算は切り捨て。`coerceIn` の下限 0 は、再起動判定を通り抜けた異常値への防御である。
- 手動リセットと再起動による破棄は本関数を呼ばない（基本設計 5.1.1）。

### 5.3 アラームの世代指定（改訂提案 R-1、R-2）

基本設計 5.5.1 は `PendingIntent` の `requestCode` を固定値 1 とし、`FLAG_UPDATE_CURRENT` で上書きすると定める。この方式には次の欠陥がある。

- `requestCode`・action・component が同一であれば、`PendingIntent` は同一と判定される。extras の違いは同一性に影響しない。したがって `AlarmManager.cancel()` は「いま登録されている予約」を無条件に解除する。
- 古い経路（アラーム受信の遅れ、復帰時の復元、手動停止の後処理）が `cancel()` を呼ぶと、その直後に開始された**新しい世代**の予約まで解除される。新しいタイマーの終了通知が発火しなくなる。
- 基本設計 5.6 の手順 1「行がなければ念のためアラームを解除する」も、解除対象の世代を指定していないため同じ問題を持つ。

本書は次の 2 段構えで対処する（詳細確定）。

**(a) `PendingIntent` の同一性に世代を含める**

```kotlin
private fun intentFor(token: Long, targetSetId: Long) =
    Intent(context, RestTimerAlarmReceiver::class.java).apply {
        action = ACTION_REST_TIMER_FINISHED
        data = "kinniku://rest-timer/$token/$targetSetId".toUri()   // 同一性はここで決まる
        putExtra(EXTRA_TOKEN, token)
        putExtra(EXTRA_TARGET_SET_ID, targetSetId)
    }
```

- `data` に世代を埋めることで、世代ごとに別の `PendingIntent` になる。`cancel(token, targetSetId)` は指定した世代だけを解除する。
- `requestCode` は固定値 1 のままでよい。同一性は action・data・component で決まるためである。
- フラグは登録時が `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`、解除時が `FLAG_NO_CREATE or FLAG_IMMUTABLE` とする。解除時に `NO_CREATE` を使うことで、存在しない予約に対して新規作成しない。

**(b) 最後に登録した世代を記録する**

- `AlarmSchedulerImpl` は、登録の直前に `last_alarm_token` と `last_alarm_target_set_id` を DataStore へ書く（本書 2.8）。解除の直後に消す。
- `rest_timer` に行がない状態で取り残された予約を解除する場合（基本設計 5.6 の手順 1）は、この記録から世代を復元して解除する。世代が分からないまま無条件に解除しない。

**(c) 順序の直列化**

- `RestTimerCoordinator` は `Mutex` を 1 つ持ち、DB 操作からアラーム操作までを含むすべての公開メソッドをその配下で実行する。
- これにより「完了処理が DB を更新した直後、アラームを解除する前に、別経路が新しいタイマーを開始する」という割り込みが起きない。
- (a) と (c) は独立した防御である。(a) だけでも新世代の誤解除は防げるが、(c) を併用することで、DB とアラームの整合が崩れる時間窓そのものを無くす。

### 5.4 AlarmScheduler

```kotlin
interface AlarmScheduler {
    /** fireAtElapsedMillis は SystemClock.elapsedRealtime() 基準の絶対値 */
    suspend fun schedule(token: Long, targetSetId: Long, fireAtElapsedMillis: Long): ScheduleOutcome

    /** 指定した世代だけを解除する */
    suspend fun cancel(token: Long, targetSetId: Long)

    /** 取り残された予約を、記録された最終世代から解除する */
    suspend fun cancelLastKnown()

    fun canScheduleExact(): Boolean
}

enum class ScheduleOutcome { EXACT, INEXACT, FAILED }
```

| 項目 | 値 |
|---|---|
| `requestCode` | 固定値 1 |
| 同一性 | action + `data`（`kinniku://rest-timer/{token}/{targetSetId}`）+ component |
| 登録フラグ | `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE` |
| 解除フラグ | `FLAG_NO_CREATE or FLAG_IMMUTABLE` |
| アラーム種別 | `AlarmManager.ELAPSED_REALTIME_WAKEUP` |
| API（正確） | `setExactAndAllowWhileIdle` |
| API（フォールバック） | `setAndAllowWhileIdle` |

```
schedule(token, targetSetId, fireAt):
    settings.setLastAlarm(token, targetSetId)          -- 登録の前に記録する
    pi = PendingIntent.getBroadcast(ctx, 1, intentFor(token, targetSetId),
                                    UPDATE_CURRENT or IMMUTABLE)
    if (canScheduleExact()) {
        try { setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, fireAt, pi); return EXACT }
        catch (e: SecurityException) { /* 下へ落ちる */ }
    }
    try { setAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, fireAt, pi); return INEXACT }
    catch (e: Exception) { return FAILED }

cancel(token, targetSetId):
    pi = PendingIntent.getBroadcast(ctx, 1, intentFor(token, targetSetId),
                                    NO_CREATE or IMMUTABLE)
    if (pi != null) { alarmManager.cancel(pi); pi.cancel() }
    settings.clearLastAlarmIfMatches(token, targetSetId)

cancelLastKnown():
    last = settings.lastAlarm() ?: return
    cancel(last.token, last.targetSetId)
```

- 記録を登録の**前**に書く。書いた直後にプロセスが落ちても、余分な解除が 1 回走るだけで害はない。逆順にすると、登録後・記録前に落ちた場合に解除できない予約が残る。
- `ScheduleOutcome.FAILED` の扱いは 5.7 に定める。

### 5.5 RestTimerCoordinator

```kotlin
class RestTimerCoordinator @Inject constructor(
    private val repo: RestTimerRepository,
    private val alarms: AlarmScheduler,
    private val notifier: RestNotifier,
    private val time: TimeProvider,
) {
    private val mutex = Mutex()

    val readiness: StateFlow<TimerReadiness>          // 5.8

    suspend fun completeRestTimer(token: Long, targetSetId: Long): Boolean
    suspend fun applyHandover(handover: TimerHandover): ScheduleOutcome
    suspend fun stopManually(): AppResult<Unit>
    suspend fun resetManually(): AppResult<Unit>
    suspend fun cancelAlarmIfNeeded(cancellation: TimerCancellation)
    suspend fun restoreOnStart()
}
```

すべての公開メソッドは `mutex.withLock { ... }` の中で実行する（5.3(c)）。

#### 5.5.1 completeRestTimer

リアルタイム検知の 2 経路（フォアグラウンド監視、アラーム受信）が呼ぶ。

```
completeRestTimer(token, targetSetId):                      -- mutex 配下
    state = repo.get() ?: return false
    if (state.token != token || state.targetSetId != targetSetId) return false
    restSec = RestSecCalculator.recordedRestSec(time.elapsedRealtimeMillis(), state)
    deleted = repo.completeMatching(token, targetSetId, restSec)   -- 単一トランザクション
    if (!deleted) return false
    notifier.notifyFinished()                 -- 権限がなければ内部で何もしない
    alarms.cancel(token, targetSetId)         -- この世代だけを解除する
    return true
```

- 先読みの `repo.get()` は `restSec` の算出に必要な値を得るためだけに行う。判定の根拠にはしない。判定はトランザクション内の照合と削除件数で決まる（基本設計 5.1.2）。
- 削除に成功した経路だけが通知する。フォアグラウンド監視とアラーム受信が同時に走っても通知は 1 回（基本設計 5.1.2）。
- `alarms.cancel` に世代を渡す。無条件解除を行わない。

#### 5.5.2 applyHandover

セット確定の直後に呼ぶ。基本設計 5.5.3「開始」の後続処理にあたる。

```
applyHandover(handover):                                    -- mutex 配下
    when (handover.cancel) {
        is Cancel -> alarms.cancel(cancel.token, cancel.targetSetId)   -- 旧世代
        None      -> {}
    }
    outcome = alarms.schedule(handover.newToken, handover.newTargetSetId,
                              handover.fireAtElapsedMillis)
    return outcome
```

- 旧世代の解除を先に行う。世代が違うため、新世代の登録が先でも壊れないが、順序を固定して挙動を単純にする。
- DB の更新は呼び出し元（`RecordRepository.commitFirstStrengthSet`）のトランザクションで完了済みである。ここでは OS 側の予約だけを操作する（基本設計 5.5.3「DB を先に更新し、OS への操作を後に行う」）。
- 戻り値の `ScheduleOutcome` は呼び出し元の `CommitStrengthSetUseCase` が受け取り、`FAILED` なら UI へ縮退を知らせる（5.7）。

#### 5.5.3 stopManually / resetManually

```
stopManually():                                             -- mutex 配下
    state = repo.get() ?: return Failure(TargetGone)
    restSec = RestSecCalculator.recordedRestSec(time.elapsedRealtimeMillis(), state)
    ok = repo.stopMatching(state.token, state.targetSetId, restSec)
    if (ok) alarms.cancel(state.token, state.targetSetId)
    return Success                       -- ok が false でも成功扱い（既に終わっている）

resetManually():                                            -- mutex 配下
    state = repo.get() ?: return Failure(TargetGone)
    ok = repo.resetMatching(state.token, state.targetSetId)
    if (ok) alarms.cancel(state.token, state.targetSetId)
    return Success
```

- `ok` が偽の場合、他経路が既に完了させている。アラームはその経路が解除済みである。ここで解除しない。
- リセットは `rest_sec` を書かない。自動で再開しない（要件 4.4.1）。
- どちらも通知を出さない（基本設計 5.7）。

#### 5.5.4 cancelAlarmIfNeeded

削除操作（セット・種目記録・セッション）の後続処理。

```
cancelAlarmIfNeeded(cancellation):                          -- mutex 配下
    when (cancellation) {
        is Cancel -> alarms.cancel(cancellation.token, cancellation.targetSetId)
        None      -> {}
    }
```

- 世代は削除の直前に DB から取得済みである（本書 3.6）。CASCADE でタイマー行が消えた後でも、控えた世代で正しく解除できる。

#### 5.5.5 restoreOnStart

基本設計 5.6 を実装する。レビュー指摘に従い、**DB 更新に成功した場合のみ、その世代のアラームを解除する**。

```
restoreOnStart():                                           -- mutex 配下
    readiness = Restoring
    try {
        state = repo.get()
        if (state == null) {
            alarms.cancelLastKnown()          -- 記録された最終世代のみを解除（5.3(b)）
            readiness = Ready; return
        }

        now = time.elapsedRealtimeMillis()
        rebooted = state.startedBootCount == TimeProvider.BOOT_COUNT_UNKNOWN
                || time.bootCount() == TimeProvider.BOOT_COUNT_UNKNOWN
                || time.bootCount() != state.startedBootCount
                || now < state.startedElapsedMillis
        if (rebooted) {
            if (repo.discardMatching(state.token, state.targetSetId)) {
                alarms.cancel(state.token, state.targetSetId)
            }
            readiness = Ready; return
        }

        if (RestSecCalculator.elapsedSec(now, state.startedElapsedMillis) >= state.targetSec) {
            restSec = RestSecCalculator.recordedRestSec(now, state)
            if (repo.completeMatching(state.token, state.targetSetId, restSec)) {
                alarms.cancel(state.token, state.targetSetId)
            }
            readiness = Ready; return         -- 通知は表示しない（基本設計 5.6）
        }

        remaining = state.targetSec * 1000L - (now - state.startedElapsedMillis)
        alarms.schedule(state.token, state.targetSetId, now + remaining)
        readiness = Ready
    } catch (e: Throwable) {
        readiness = Failed
        throw e
    }
```

- `discardMatching` / `completeMatching` が偽を返した場合、対象は既に別世代へ入れ替わっている。**アラームを解除しない**。解除すると新世代の予約を消す。
- `state == null` のときの解除は `cancelLastKnown()` に限る。無条件の `cancel()` は行わない。
- 再起動の判定に `BOOT_COUNT_UNKNOWN` の条件を含める。保存値と現在値のどちらかが判定不能なら破棄する（本書 1.4、改訂提案 R-4）。
- 手順 3 では `notifier` を呼ばない。基本設計 5.6 の「復帰時の事後確定では終了通知を表示しない」に従う。
- 再登録は世代が同じであるため、既存の予約があれば `FLAG_UPDATE_CURRENT` で上書きされ、重複しない。
- 呼び出し箇所は `MainActivity.onStart()` の 1 か所とする（本書 6.4）。`Application.onCreate` から呼ぶと、`BroadcastReceiver` によるプロセス起動時にも走り、受信処理と競合する。

### 5.6 RestTimerAlarmReceiver

```kotlin
@AndroidEntryPoint
class RestTimerAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var coordinator: RestTimerCoordinator
    @Inject lateinit var dispatchers: DispatcherProvider

    override fun onReceive(context: Context, intent: Intent) {
        val token = intent.getLongExtra(EXTRA_TOKEN, INVALID)
        val setId = intent.getLongExtra(EXTRA_TARGET_SET_ID, INVALID)
        if (token == INVALID || setId == INVALID) return
        val pending = goAsync()
        CoroutineScope(dispatchers.io).launch {
            try {
                coordinator.completeRestTimer(token, setId)
            } catch (c: CancellationException) {
                throw c
            } catch (t: Throwable) {
                Logger.timerFailure(t)          // 秘匿ログ。値を含めない（10.3）
            } finally {
                pending.finish()
            }
        }
    }
}
```

- 受信側は判定・書き込み・通知を持たない。共通ルーチンを呼ぶだけとする（基本設計 5.5.2）。
- `CancellationException` は再スローし、それ以外は捕捉する。捕捉しないとプロセスが落ち、`finish()` も呼ばれない。
- 例外時も休憩時間は次回起動時の復元処理（5.5.5）で確定するため失われない。
- ログには `token` と `targetSetId` のみを出し、休憩時間・種目名・メモを出さない（本書 10.3）。
- マニフェスト宣言は `android:exported="false"`。

### 5.7 アラーム登録失敗時の縮退

`ScheduleOutcome` ごとの扱いを定める（詳細確定。レビュー指摘に対応）。

| 結果 | UI への通知 | 縮退動作 |
|---|---|---|
| `EXACT` | なし | 通常動作 |
| `INEXACT` | S-11 に「終了通知が遅れる場合があります」を常時表示（基本設計 6.5.5） | 通知が遅れる可能性がある |
| `FAILED` | S-01 にスナックバー「終了通知を設定できませんでした。アプリを開いている間は動作します」 | 下記 |

`FAILED` のときの縮退（3 段構え）。

1. タイマー行は削除しない。計測は継続する。
2. アプリが前面にある間は、フォアグラウンド監視（5.9）が目安秒数への到達を検知して確定し、通知も表示する。
3. 前面にない間に到達した場合は、次回起動時の復元処理（5.5.5）が休憩時間を確定する。通知は出ない。

- `FAILED` は `USE_EXACT_ALARM` が通常権限として自動付与される環境では発生しない想定である。それでも利用者が「通知が来ない」ことを知らないまま待つ状態を作らないよう、スナックバーで明示する。
- ログには失敗した事実のみを残す。

### 5.8 起動状態のゲート

レビュー指摘に対応し、復元が終わるまでタイマーを変更する操作を受け付けない（詳細確定）。

```kotlin
enum class TimerReadiness { RESTORING, READY, FAILED }
```

| 状態 | 意味 | UI の扱い |
|---|---|---|
| `RESTORING` | `restoreOnStart` の実行中。初期値 | セット完了・停止・リセットのボタンを無効化する。ticker を開始しない |
| `READY` | 復元完了 | 通常動作 |
| `FAILED` | 復元中に例外 | タイマー操作を無効のままとし、S-01 にスナックバーで再試行を案内する |

- `RestTimerCoordinator.readiness` を `StateFlow` として公開し、S-01 の `UiState` に取り込む。
- `RESTORING` の間にタイマー変更操作が呼ばれた場合、Repository へ到達する前に `AppError.TimerNotReady` を返す。
- 復元は通常ミリ秒単位で終わる。ゲートが利用者に見えるのは、初回起動で DB を作る場合などに限られる。
- ticker（5.9）は `readiness == READY` になってから購読を開始する。復元前の状態を根拠に完了処理を走らせないため。

### 5.9 フォアグラウンド監視

```kotlin
fun observeTimerTick(): Flow<RestTimerUiModel?>

data class RestTimerUiModel(
    val token: Long,
    val targetSetId: Long,
    val targetSec: Int,
    val elapsedSec: Int,          // 表示用。targetSec で頭打ち
    val remainingSec: Int,
    val displayText: String,      // mm:ss（M-5）
)
```

- `RestTimerRepository.observe()` を `flatMapLatest` し、行がある間だけ 1 秒周期で値を流す。行が消えたら `null` を流して停止する。
- 各 tick で `elapsedSec` を算出する。`target_sec` 未満なら表示のみ更新する。
- `target_sec` 以上になった時点で `coordinator.completeRestTimer(token, targetSetId)` を 1 回だけ呼ぶ。呼び出し済みフラグを `flatMapLatest` のスコープ内に持ち、同一世代で二重に呼ばない（詳細確定）。
- 表示の経過秒は `min(elapsedSec, targetSec)` とする。`target_sec` を超えた値を画面に出さない（基本設計 5.1）。
- 所有者は S-01 の ViewModel とする。画面が背面にある間は購読が止まり tick も止まるが、到達の確定はアラーム受信が担う。
- `readiness == READY` の間だけ購読する（5.8）。

### 5.10 通知

`RestNotifierImpl` の仕様（詳細確定。基本設計 5.8 の範囲内）。

| 項目 | 値 |
|---|---|
| チャンネル ID | `rest_timer` |
| チャンネル名 | 休憩タイマー |
| 重要度 | `IMPORTANCE_LOW` |
| 音・バイブレーション | `setSound(null, null)`、`enableVibration(false)` |
| 通知 ID | 固定値 1001 |
| タイトル | 休憩終了 |
| 本文 | 目安の休憩時間になりました |
| タップ動作 | `MainActivity` を `FLAG_ACTIVITY_SINGLE_TOP` で開き、`today` を表示する（16 章 D-5） |
| 自動消去 | `setAutoCancel(true)` |
| 小アイコン | アプリのモノクロアイコン |

- `IMPORTANCE_LOW` は通知バーへ表示しつつ、音・バイブレーション・ヘッドアップ表示を伴わない。要件 4.5 の「通知バーへの表示のみ」に一致する。
- チャンネルの作成は `KinnikuApplication.onCreate` で行う。作成済みチャンネルへの再作成は無害である。
- 通知本文に種目名・重量・回数を含めない。通知バーは他者から見えうるため（本書 10.3）。

#### 5.10.1 通知可否の判定

レビュー指摘に対応し、ランタイム権限とチャンネル無効を別々に判定する（詳細確定）。

```kotlin
data class NotificationStatus(
    val permissionGranted: Boolean,      // POST_NOTIFICATIONS のランタイム権限
    val appNotificationsEnabled: Boolean, // アプリ全体の通知が有効か
    val channelEnabled: Boolean,          // rest_timer チャンネルが有効か
) {
    val canNotify: Boolean
        get() = permissionGranted && appNotificationsEnabled && channelEnabled
}
```

| 判定 | 取得方法 | 用途 |
|---|---|---|
| `permissionGranted` | `ContextCompat.checkSelfPermission(POST_NOTIFICATIONS)` | 権限要求を出すかどうか |
| `appNotificationsEnabled` | `NotificationManagerCompat.areNotificationsEnabled()` | OS 設定への導線を出すかどうか |
| `channelEnabled` | `getNotificationChannel("rest_timer")?.importance != IMPORTANCE_NONE` | 同上 |

- 権限要求を出す条件は `permissionGranted == false` かつ `notification_permission_requested == false` のみとする。`areNotificationsEnabled()` を条件に含めない。権限は許可済みでチャンネルだけ無効な場合に、要求ダイアログを出しても何も起きないためである。
- `permissionGranted == true` かつ `canNotify == false` の場合は、権限要求を出さず、S-11 に OS 設定への導線を表示する。
- `notifyFinished()` は `canNotify` が偽なら何もせず戻る。計測と `rest_sec` の記録は通常どおり行う（要件 4.5）。

#### 5.10.2 権限要求フロー

基本設計 6.5.4 の順序を実装する。

1. `CommitStrengthSetUseCase` がセット確定・タイマー開始・アラーム登録を終える。
2. `permissionGranted == false` かつ `notification_permission_requested == false` のときのみ、`Effect.RequestNotificationPermission` を発行する。
3. S-01 が説明ダイアログを表示する。利用者が同意した場合のみ `ActivityResultContracts.RequestPermission` を起動する。
4. 結果に関わらず `markNotificationRequested()` を呼ぶ。再要求しない（基本設計 5.8）。同意しなかった場合も記録する。
5. 進行中のタイマーには影響しない。許可・拒否のどちらでも計測と記録は継続する。

---

## 6. UI 層詳細設計

### 6.1 共通構造

各画面は次の 4 要素で構成する（詳細確定）。

| 要素 | 内容 |
|---|---|
| `XxxUiState` | 画面の全表示状態を持つ単一のデータクラス。`StateFlow` で公開する |
| `XxxEvent` | 利用者操作を表す sealed interface |
| `XxxEffect` | 1 回限りの副作用（遷移、スナックバー、権限要求） |
| `XxxScreen` | 状態を描画し、操作を `Event` として送るだけの Composable |

```kotlin
interface BaseUiState {
    val loadState: LoadState        // Loading / Ready / Failed
    val inFlight: Boolean           // 保存・削除・適用・出力の実行中
}
```

#### 6.1.1 二重操作・二重遷移の防止

レビュー指摘に対応し、説明を訂正する（詳細確定）。`Effect` を `Channel(Channel.BUFFERED)` で公開しても、イベントは**バッファへ積まれる**。チャネル自体が二重発行を防ぐわけではない。防止は次の 3 点で担保する。

1. **`inFlight` フラグ**。保存・削除・適用・出力・遷移を伴う操作は、`Event` を受けた直後に `inFlight = true` とし、対象ボタンを無効化する。完了または失敗まで戻さない。これが一次防御である。
2. **`launchSingleTop = true`**。`NavController` の遷移オプションで指定する。同一ルートが連続で積まれない。
3. **消費済み判定**。`Effect` は `receiveAsFlow()` で 1 回だけ消費する。`collect` は `repeatOnLifecycle(STARTED)` の中で行い、画面が背面のときに消費しない。

- 遷移を伴う操作では、`inFlight` を戻すのは遷移先が composed された後ではなく、`Effect` を発行した直後とする。戻り操作で元の画面に戻ったときにボタンが無効のままになることを避けるためである。二重遷移は上記 2 で防ぐ。
- `Channel` の容量は `Channel.BUFFERED` とする。取りこぼしを避けるためであり、二重防止の手段ではない。

#### 6.1.2 読み込み状態の表示

```kotlin
sealed interface LoadState {
    data object Loading : LoadState
    data object Ready : LoadState
    data class Failed(val message: UiText) : LoadState
}
```

- Repository の `LoadResult.Failed` を受けた場合、直前の正常値を表示したままエラーバナーを重ねる（本書 1.6）。空状態の文言へ差し替えない。
- 検証エラーは `UiState` 内の `Map<FieldId, ValidationKind>` として保持し、該当入力欄の直下に表示する（基本設計 6.3）。

### 6.2 Navigation

| ルート | 引数 | 画面 |
|---|---|---|
| `today` | なし | S-01 |
| `calendar` | なし | S-05 |
| `graph` | なし | S-07 |
| `weight` | なし | S-08 |
| `exercise_picker/{mode}?sessionId={sessionId}` | `mode`: `record` / `template` | S-02 |
| `exercise_master` | なし | S-03 |
| `template?sessionId={sessionId}` | `sessionId`: 省略可 | S-04 |
| `history/{epochDay}` | `epochDay`: Long | S-06 |
| `summary` | なし | S-09 |
| `csv` | なし | S-10 |
| `settings` | なし | S-11 |

- ボトムナビゲーションのタブは `today` / `calendar` / `graph` / `weight` の 4 つ（Q-1）。タブ切替は `launchSingleTop = true`、`popUpTo(startDestination) { saveState = true }`、`restoreState = true` を指定する。
- 管理系 5 画面（S-03、S-04、S-09、S-10、S-11）は S-01 のトップアプリバーのオーバーフローメニューから遷移する（基本設計 6.2）。
- `SessionTarget` を sealed 型のまま引数へ符号化しない。`sessionId` が省略された場合を `AutoCreateOnCommit`、指定された場合を `Existing(sessionId)` として復元する（基本設計 3.2.1）。

#### 6.2.1 テンプレート適用先の受け渡し

レビュー指摘に対応し、`template` ルートに `sessionId` を追加する（詳細確定）。

| 起点 | 遷移 | 適用先の決定 |
|---|---|---|
| S-01 の特定セッションから適用 | `template?sessionId={id}` | `Existing(id)` |
| S-01 でその日のセッションが 0 件 | `template`（引数なし） | `AutoCreateOnCommit` |
| メニューから S-04 を直接開く | `template`（引数なし） | 当日のセッションが 1 件以上あれば最後のセッション、0 件なら `AutoCreateOnCommit`（Q-9） |

結果の受け渡し。

```kotlin
object NavResultKeys {
    const val PICKED_EXERCISE_ID = "picked_exercise_id"
    const val APPLIED_TEMPLATE = "applied_template"
}

@Parcelize
data class ApplyTemplateResult(
    val templateId: Long,
    val sessionId: Long?,        // null は AutoCreateOnCommit
) : Parcelable
```

- S-02 と S-04 は結果を、遷移元の `NavBackStackEntry.savedStateHandle` へ書いてから `popBackStack()` する。
- S-01 は該当キーを `Flow` で購読し、値を受け取ったら即座に `remove` する。同じ結果を二重に処理しない。
- `ApplyTemplateResult` が適用先を持つため、S-01 は「どのセッションへ展開するか」を推測しない。

### 6.3 画面ごとの状態と処理

#### 6.3.1 S-01 今日の記録

```kotlin
data class TodayUiState(
    override val loadState: LoadState = LoadState.Loading,
    override val inFlight: Boolean = false,
    val epochDay: Long,
    val dateHeader: String,                       // M-3
    val totalVolumeText: String,                  // M-15
    val sessions: List<SessionUi>,
    val drafts: List<DraftUi>,
    val timer: RestTimerUiModel?,                 // M-5
    val timerReadiness: TimerReadiness,           // 5.8
    val errors: Map<FieldId, ValidationKind> = emptyMap(),
) : BaseUiState

data class DraftUi(
    val draftId: DraftId,
    val target: SessionTarget,
    val exerciseId: Long,
    val displayName: String,                      // 表示用のスナップショット
    val bodyPart: BodyPart,
    val category: ExerciseCategory,
    val isBodyweight: Boolean,
    val recordId: Long?,                          // 1 セット目の確定で埋まる
    val setInputs: List<SetInput>,                // 筋トレ。未確定の入力行のみ
    val durationInput: String?,                   // 有酸素
)
```

**下書きの状態遷移**（レビュー指摘に対応。詳細確定）

| # | 現在の状態 | 契機 | 遷移後 | 呼ぶ Repository メソッド |
|---|---|---|---|---|
| 1 | `recordId = null`、`setInputs` に N 行 | 行 `i` を完了 | `recordId = R` を保存。`setInputs` から行 `i` を除去。確定済みへ種目記録 `R` とセット 1 件が現れる | `commitFirstStrengthSet` |
| 2 | `recordId = R`、`setInputs` に残り行 | 行 `j` を完了 | `setInputs` から行 `j` を除去。確定済みの `R` にセットが 1 件増える | `appendStrengthSet(R, ...)` |
| 3 | `recordId = R`、`setInputs` が空 | ― | 下書き行を一覧から消す。確定済みの `R` のみ残る | なし |
| 4 | 任意 | 「セットを追加」 | `setInputs` に空行を 1 つ足す | なし |
| 5 | 任意 | 下書きの破棄 | 下書きを消す。確定済みの `R` は残る（DB 上の記録であるため） | なし |
| 6 | `recordId = null`、`setInputs` が空 | 下書きの破棄 | 下書きを消す。DB は変わらない | なし |
| 7 | 有酸素、`durationInput` | 完了 | 下書きを消す。確定済みへ種目記録が現れる | `commitCardioRecord` |

- 手順 1 の失敗時は `recordId` を保存せず、下書きをそのまま残す。エラーを入力欄へ表示する。
- 手順 2 で `appendStrengthSet` が `TargetGone` を返した場合（種目記録が別画面から削除された）、下書きの `recordId` を `null` へ戻し、次回の完了で新しい種目記録を作る。
- テンプレート適用では、`set_count` の数だけ `setInputs` に空行を作る。行ごとに順次確定する。未入力のまま画面を離れた行は保存されない（要件 4.2）。
- 同一種目を同一セッションへ重複して追加できる（要件 4.6）。下書きごとに別の種目記録が作られる。

**主なイベントと処理**

| イベント | 処理 |
|---|---|
| `CompleteSet(draftId, index)` | 検証 → `CommitStrengthSetUseCase`（上表の 1 または 2）→ `applyHandover` → 通知権限の要否判定 |
| `CommitCardio(draftId)` | 検証 → `CommitCardioRecordUseCase` |
| `AddSetInput(draftId)` | 上表の 4 |
| `EditSet(setId)` | 当日判定 → 重量・回数の編集ダイアログ（メモは対象外、D-1） |
| `EditCardioDuration(recordId)` | 当日判定 → 実施時間の編集ダイアログ（要件 4.3.1） |
| `EditSessionName(sessionId)` | 当日判定 → セッション名の編集ダイアログ（要件 4.3.1） |
| `MoveRecord` / `MoveSet` | 当日判定 → `moveRecord` / `moveSet`（M-9） |
| `DeleteSet(setId)` | 確認なしで削除（要件 4.3.2）→ `cancelAlarmIfNeeded` |
| `DeleteRecord` / `DeleteSession` | 確認ダイアログ → 削除 → `cancelAlarmIfNeeded` |
| `DiscardDraft(draftId)` | 確認なしで下書きを破棄（基本設計 3.3） |
| `StopTimer` / `ResetTimer` | `readiness == READY` のときのみ実行 |
| `AddSession` | `createSession(today, null)`（Q-7） |

- 並び替えは有酸素の種目記録も対象とする（16 章 D-2）。セットの並び替えは筋トレのみに存在する。
- 0 セットの種目記録は「種目名のみの行」として表示し、セット追加と種目記録削除の両方を案内する（Q-11）。
- 空状態の文言は「まだ記録がありません」（M-1）。
- 端末の戻る操作でアプリを終了する。`BackHandler` を置かない。

**日付の追従**

- `DateChangeNotifier.observeToday()`（本書 4.7）を購読し、値が変わったら `epochDay` を新しい当日へ切り替えて再読み込みする。
- 加えて、保存系イベントの先頭で `TimeProvider.today()` と `state.epochDay` を比較する。異なる場合は保存せず、スナックバーを出して再読み込みする（基本設計 4.1）。
- 下書きは日付が変わっても破棄しない。下書きは DB 上の記録ではなく、確定時に当日のセッションへ結び付く（基本設計 3.3）。

**下書きの退避**

レビュー指摘に対応し、保存専用の DTO と上限を定める（詳細確定）。

```kotlin
@Parcelize
data class DraftSnapshotDto(
    val draftId: String,
    val sessionId: Long?,          // null は AutoCreateOnCommit
    val exerciseId: Long,
    val recordId: Long?,
    val setInputs: List<SetInputDto>,
    val durationInput: String?,
) : Parcelable

@Parcelize
data class SetInputDto(val weight: String, val reps: String, val memo: String) : Parcelable
```

| 項目 | 上限 | 超過時 |
|---|---|---|
| 下書きの件数 | 20 件 | 古いものから捨てる |
| 1 下書きの入力行 | 20 行 | 21 行目以降を捨てる |
| メモ 1 件の文字数 | 500 文字（要件 3.10） | 検証で弾くため超過しない |
| 退避データの総量 | 概ね 100 KB 未満 | 上記 2 つの上限により自然に収まる |

- 表示用の種目名・部位・自重フラグは退避しない。`exerciseId` から復元時に引き直す。種目が削除済みなら、その下書きを破棄する。
- 復元に失敗した場合（`ClassCastException`、`exerciseId` が解決できないなど）は、**全下書きを破棄**して空から始める。部分復元を試みない。下書きは DB 上の記録ではなく、失われても記録は損なわれない（Q-14）。
- タスク一覧からの除去では失われる。要件 4.2 が未完了の入力を保存しないと定めているため許容する（Q-14）。

#### 6.3.2 S-02 種目選択

```kotlin
data class PickerUiState(
    override val loadState: LoadState = LoadState.Loading,
    override val inFlight: Boolean = false,
    val query: String = "",
    val partFilter: BodyPart? = null,             // Q-2
    val items: List<ExerciseWithUsageCount>,
) : BaseUiState
```

- 一覧は `observeActiveWithUsage()`。並びは累計使用回数の降順、同数は種目マスタ ID の昇順（要件 4.1、M-6、M-19）。
- 絞り込みと検索はメモリ上で行う。有効種目は多くても数百件であり、SQL の `LIKE` を用いない（詳細確定）。検索は `NameKey.of()` を適用した文字列どうしの部分一致とする。全角・半角と大文字・小文字の違いを吸収する（Q-2）。
- 選択後は結果を `savedStateHandle` へ書いて即座に `popBackStack()` する。連続選択を受け付けない。
- 空状態は、検索結果 0 件で「該当する種目がありません」、全件 0 件で「種目がありません」（M-1）。
- `mode = template` の場合、選択結果はテンプレート編集画面へ返す（基本設計 6.5.2）。

#### 6.3.3 S-03 種目マスタ管理

- 一覧は `observeActive()`。並びは ID 昇順（M-6）。
- 追加・編集はダイアログで行う。
- 区分と部位の連動（Q-3、基本設計 6.5.1）。
  - 区分「有酸素」を選ぶと部位を `CARDIO` に固定し、自重フラグの入力を無効化して `false` にする。
  - 区分「筋トレ」を選ぶと部位の選択肢から `CARDIO` を除く。現在の部位が `CARDIO` の場合は未選択に戻す。
- 編集で区分を変更した場合、保存前に確認ダイアログを出す。文言は「この種目を含むテンプレートから取り除かれます」（要件 3.4）。
- 削除時の確認ダイアログには、テンプレートから取り除かれることと、過去記録は残ることを明記する。
- 名称の重複は保存操作の時点で検出し、入力欄の直下に表示する。

#### 6.3.4 S-04 テンプレート管理

- 一覧は `observeTemplates()`（明細を含む `TemplateWithItems`）。
- 明細 0 件のテンプレートは「種目 0 件」と表示し、適用ボタンを無効化する（M-18）。
- 新規保存時に明細 0 件は許可しない（Q-12）。既存の編集で 0 件になった場合は保存を許す。
- 適用は `ApplyTemplateUseCase` が下書きリストを生成し、`ApplyTemplateResult` を S-01 へ返す。DB へ書かない（基本設計 3.3）。
- 適用先は 6.2.1 の表に従う。
- セット数の入力欄は筋トレ種目のみ表示する。有酸素種目には表示せず `set_count` を NULL とする（要件 4.6）。

#### 6.3.5 S-05 カレンダー

- 表示月の実施日は `observeTrainingDays(monthStart, monthEnd)` を購読する。
- 当日を強調表示する。印は実施日のみ（要件 4.7）。
- 日付タップの遷移先は、当日なら `today`、過去日なら `history/{epochDay}`。未来日はタップしても遷移しない（Q-4）。
- 月の前後移動に上限を設けない。

#### 6.3.6 S-06 過去記録の参照

- 表示は S-01 と同じ構成の Composable を再利用し、編集・追加の入口を描画しない。
- 操作は 3 単位の削除と日付の前後移動のみ。
- 削除は当日・過去日を問わず可能（要件 4.3.2）。
- 0 セットの種目記録は、種目記録の削除のみを案内する（Q-11）。
- 空状態は「この日の記録はありません」（M-1）。

#### 6.3.7 S-07 種目別グラフ

```kotlin
data class GraphUiState(
    override val loadState: LoadState = LoadState.Loading,
    override val inFlight: Boolean = false,
    val selectableExercises: List<GraphExerciseItem>,
    val selectedExerciseId: Long?,
    val period: GraphPeriod = GraphPeriod.MONTH_1,
    val points: List<VolumePoint>,
    val axis: AxisSpec?,                                // 空状態では null
) : BaseUiState

data class GraphExerciseItem(val exerciseId: Long, val label: String, val isDeleted: Boolean)
data class AxisSpec(val maxKg: Int, val stepKg: Int, val labels: List<String>, val xLabels: List<String>)
```

**選択種目の決定**（レビュー指摘に対応。詳細確定）

1. 現在の `selectedExerciseId` が新しい `selectableExercises` に含まれていれば、それを維持する。
2. 含まれていなければ先頭の要素を選ぶ。
3. `selectableExercises` が空なら `null` とし、空状態を表示する。

- 選択肢は `observeGraphSelectable()`。筋トレ記録が 1 件以上ある種目に限る（改訂提案 R-6）。削除済みは名称の末尾に「（削除済み）」を添える（Q-10）。並びは種目マスタ ID の昇順（16 章 D-4）。
- 期間は `PeriodCalculator` が算出する（M-20）。
- 点が 0 件の場合は `axis` を `null` とし、軸を描画せず「対象期間に記録がありません」を表示する（M-1）。
- 描画は Compose の `Canvas` による自前実装（Q-8）。折れ線と点のみ。タッチによる値表示は要件にないため実装しない。

**軸の算出**（M-2 の具体化。レビュー指摘に対応）

```
axisSpec(points):
    maxKg = points.maxOf { it.volumeTenth } / 10.0
    step  = niceStep(maxKg / 5)
    上限  = step * 5
    ラベル = 0, step, 2*step, 3*step, 4*step, 5*step

niceStep(raw):
    候補 = { 1, 2, 5 } × 10^n  (n >= 0)
    raw 以上の最小の候補を返す。raw <= 0 なら 1 を返す
```

- 目盛の刻みは **1 kg 以上の整数**に限る。候補から `2.5` を除き、`10^n` の指数を 0 以上に制限することで、整数表示のラベルが重複しない。基本設計 M-2 は「整数表示」を求めており、刻みが 1 未満だと `0, 0, 1, 1, 2` のような重複ラベルになる。
- 最大値が 0 のとき（対象期間の全記録が自重種目で体重が未登録など）は `step = 1`、上限 5 とする。
- X 軸の日付ラベルは `9/5` 形式（M-3）。横幅に応じて間引き、最大 6 個とする。

#### 6.3.8 S-08 体重

- グラフは `observeDailyAverages` を購読して描画する（要件 4.9.2、M-14）。
- 一覧は `observeByRange` の全件を `epoch_day, created_at, id` の昇順で表示する。
- 追加は日付を指定でき、過去日も可（要件 3.8）。未来日は選べない。日付の既定値は当日。
- 編集の入口は、記録日が当日の行にのみ表示する（Q-5）。編集できるのは体重値のみで、日付は変更しない（16 章 D-3）。
- 削除は日付を問わず可能。確認ダイアログを出さない（基本設計 6.5.3）。
- 空状態は「体重の記録がありません」（M-1）。

#### 6.3.9 S-09 期間サマリー

- 初期表示は今日を含む週（基本設計 4.2）。
- 単位切替（週・月）と期間の前後移動。移動可能範囲は 4.6 に従う。記録 0 件なら前方向を無効化する（16 章 D-8）。
- 内訳は筋トレ 6 部位を常に表示し、0 の部位も 0 と表示する（基本設計 4.5）。
- 空状態は「対象期間に記録がありません」。合計 0、内訳すべて 0 を併記する（M-1）。

#### 6.3.10 S-10 CSV 書き出し

```kotlin
data class CsvUiState(
    override val loadState: LoadState = LoadState.Loading,
    override val inFlight: Boolean = false,
    val scope: CsvScope = CsvScope.ALL,
    val fromDate: LocalDate?, val toDate: LocalDate?,
    val oldestRecordDate: LocalDate?, val newestRecordDate: LocalDate?,
    val progress: CsvProgress?,
    val result: CsvResult?,
    val errors: Map<FieldId, ValidationKind> = emptyMap(),
) : BaseUiState

enum class CsvScope { ALL, RANGE }

data class CsvProgress(
    val currentFile: CsvFileKind,
    val fileIndex: Int,          // 1..3
    val writtenRows: Int,
)

data class CsvResult(val outcomes: List<CsvFileOutcome>) {
    val successCount: Int get() = outcomes.count { it is CsvFileOutcome.Success }
}

sealed interface CsvFileOutcome {
    val kind: CsvFileKind
    data class Success(override val kind: CsvFileKind, val displayName: String, val rowCount: Int) : CsvFileOutcome
    data class Failed(override val kind: CsvFileKind, val error: AppError) : CsvFileOutcome
    data class Cancelled(override val kind: CsvFileKind) : CsvFileOutcome
}
```

- 画面表示時に `PendingFileReaper.reap()` を起動する（基本設計 8.1 の手順 1）。結果は表示しない。
- 期間の既定値は `csvOldestDay()` と `csvNewestDay()`（本書 2.4.3）。記録が 0 件の場合は当日を両端とする。
- 開始日が終了日より後の場合は、実行前の検証でエラーとする（基本設計 8.6）。
- 出力中は `inFlight` を真にし、進捗表示で画面操作を遮断する。戻る操作も無効化する。`BackHandler(enabled = inFlight) {}` で吸収する。
- キャンセルは進捗表示内のボタンで受け付ける（本書 7.5）。
- 完了表示は M-8。成功時は「3 件のファイルをダウンロードに保存しました」とファイル名を併記する。一部失敗時は成功と失敗を個別に示す。
- 保存先と参照範囲の注意を画面に常時表示する（本書 10.3）。

#### 6.3.11 S-11 設定

- 目安秒数の入力。範囲 1〜3600（要件 3.9）。保存は DataStore へ。
- `NotificationStatus`（5.10.1）に応じた案内。
  - `permissionGranted == false`: 通知が許可されていない旨と、OS 設定への導線。
  - `permissionGranted == true` かつ `canNotify == false`: 通知が無効になっている旨と、OS 設定への導線。
  - `canNotify == true`: 案内を出さない。
- `canScheduleExact() == false` の場合のみ、「終了通知が遅れる場合があります」を表示する（基本設計 6.5.5）。

**OS 設定への遷移**（レビュー指摘に対応。詳細確定）

```
openNotificationSettings():
    intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    if (intent.resolveActivity(packageManager) != null) startActivity(intent)
    else {
        fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                     .setData(Uri.fromParts("package", packageName, null))
        if (fallback.resolveActivity(packageManager) != null) startActivity(fallback)
        else showSnackbar("設定画面を開けませんでした")
    }
```

- 暗黙 Intent が解決できない端末で `ActivityNotFoundException` により落ちることを防ぐ。

### 6.4 Application と Activity の責務

レビュー指摘に対応し、起動時処理の担当を分ける（詳細確定）。

| クラス | 担当 | 実行スコープ |
|---|---|---|
| `KinnikuApplication.onCreate` | 通知チャンネルの作成、`PendingFileReaper.reap()` の起動 | `@ApplicationScope CoroutineScope`（本書 1.5） |
| `MainActivity.onStart` | `RestoreRestTimerUseCase`（タイマーの復元・再照合） | `lifecycleScope` |

- タイマーの復元を `Application` から行わない。`BroadcastReceiver` によるプロセス起動時にも走り、受信処理と競合するためである（本書 5.5.5）。
- 未完成 CSV の回収を `MainActivity` から行わない。画面の生存期間に縛られず、`dispatchers.csv` で直列化するためである。
- `MainActivity` はマニフェストで `android:screenOrientation="portrait"`。`configChanges` は指定しない。
- テーマはライトのみ。`values-night` を作らない（基本設計 10 章）。

### 6.5 表示フォーマッタ

`ui/format` に純粋関数として置く。画面と CSV で規則を共有する項目は同一の関数を呼ぶ（M-4）。

| 関数 | 規則 | 出典 |
|---|---|---|
| `formatDateHeader(date)` | `2026年9月5日(金)` | M-3 |
| `formatDateShort(date)` | `9/5` | M-3 |
| `formatDateIso(date)` | `2026-09-05` | 要件 4.11.1 |
| `formatWeight(tenth)` | 小数部が 0 なら整数（`60`）、それ以外は小数第 1 位（`62.5`） | M-4 |
| `formatBodyWeight(tenth)` | 常に小数第 1 位（`60.5`） | 要件 4.11.4 |
| `formatDuration(sec)` | `mm:ss`。3600 秒は `60:00` | M-5 |
| `formatVolume(tenth)` | 小数第 1 位まで。3 桁区切りを入れる（`12,345.5`） | M-15、D-7 |

```kotlin
private val VOLUME_SYMBOLS = DecimalFormatSymbols(Locale.ROOT).apply {
    groupingSeparator = ','
    decimalSeparator = '.'
}
private val VOLUME_FORMAT = DecimalFormat("#,##0.0", VOLUME_SYMBOLS)
```

- `formatVolume` の記号は `Locale.ROOT` 由来に固定する。端末ロケールによって区切り文字が変わると、表示とテスト結果が一致しなくなる（16 章 D-7）。
- `formatWeight` と `formatBodyWeight` は `BigDecimal(tenth).movePointLeft(1)` を経由する。`Double` を介さない（基本設計 1.5）。
- `formatDuration` の `mm` は 2 桁ゼロ詰め。60 分を超える値は目安秒数の上限（3600 秒）により発生しない。
- 曜日の日本語表記は `DayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPAN)` を用いる。日付の表示はロケール依存の書式を使わず、上記の固定書式で組み立てる。

---

## 7. CSV 出力詳細設計

### 7.1 CsvExporter

```kotlin
class CsvExporter @Inject constructor(
    private val csvDao: CsvDao,
    private val mediaStore: MediaStoreGateway,
    private val time: TimeProvider,
    private val dispatchers: DispatcherProvider,
) {
    fun export(range: DateRange): Flow<CsvExportEvent>
}

sealed interface CsvExportEvent {
    data class Progress(val kind: CsvFileKind, val fileIndex: Int, val writtenRows: Int) : CsvExportEvent
    data class FileFinished(val outcome: CsvFileOutcome) : CsvExportEvent
    data class AllDone(val result: CsvResult) : CsvExportEvent
}

enum class CsvFileKind { STRENGTH, CARDIO, WEIGHT }
```

- `export` は `dispatchers.csv` で実行する（基本設計 8.8）。未完成ファイルの回収と同じディスパッチャであり、両者は直列化される。
- 3 ファイルを順に処理する。1 ファイルが失敗しても残りは続行する（基本設計 8.6）。
- 3 ファイルのタイムスタンプは、処理開始時に 1 度だけ算出して共有する（基本設計 8.2）。
- 体重の解決（本書 4.3）を用いない。CSV は総ボリュームを出力しないため、自重種目でも体重を参照する必要がない（要件 4.11.2）。

### 7.2 出力範囲のスナップショット

レビュー指摘に対応し、断定を改める（詳細確定）。

- ページごとにトランザクションを閉じるため、**出力全体は単一時点のスナップショットではない**。
- 出力開始時に `maxSessionKey(from, to)` と `maxWeightKey(from, to)` を 1 度だけ取得し、全ページの上限キーとして固定する（本書 2.4.10）。これにより、**出力中に追加された行は出力されない**。
- 出力中に削除された行は、まだ読んでいなければ出力されない。既に読んで書き出していれば出力に残る。この差は許容する。
- 出力中は進捗表示で画面操作を遮断するため、利用者による追加・削除は起こらない（基本設計 8.6）。出力中に走り得る唯一の書き込みは休憩タイマーによる `rest_sec` の更新であり、CSV の出力対象外のため内容に影響しない（要件 4.11.2）。
- 「ページ間でデータが増減しても取りこぼしや重複が起きない」とは書かない。上限キーの固定により追加の混入は防げるが、削除に対する一貫性までは保証しない。

### 7.3 出力手順

1. `time.nowLocalDateTime()` から `uuuuMMdd_HHmmss` を作る。3 ファイルで共有する。
2. 上限キーを 2 本取得する（7.2）。
3. 筋トレ、有酸素、体重の順に、次を繰り返す。
   1. MediaStore へ `IS_PENDING = 1` でエントリを登録し、その `Uri` を保持する（7.5）。
   2. `OutputStream` を開き、UTF-8 BOM（`EF BB BF`）を書く。
   3. ヘッダー行を書く。
   4. ページを 1 つ取得する（`@Transaction`）。取得後にトランザクションを閉じる。
   5. 取得したページを行へ変換し、`OutputStream` へ書く。トランザクションの外で行う。
   6. ページが空になるまで手順 4 と 5 を繰り返す。
   7. `flush` し、`IS_PENDING = 0` にして確定する。保持していた `Uri` を解放する。
4. すべての結果を `AllDone` で返す。

- 手順 3-4 と 3-5 を分けることが、基本設計 8.8 の「読み取り接続を保持したままファイル入出力を行わない」に対応する。
- 筋トレファイルは、同一ページ内で種目記録ごとにセットをまとめてから 1 行を組み立てる。ページをまたぐ種目記録は生じない。ページの単位がセッションであり、セッションに属する種目記録とセットは同じページで取得されるためである。

### 7.4 ファイルごとの行生成

```kotlin
object CsvSpec {
    val STRENGTH_HEADER = listOf("日付", "セッション名", "種目", "部位", "セット数", "セット詳細")
    val CARDIO_HEADER   = listOf("日付", "セッション名", "種目", "実施時間")
    val WEIGHT_HEADER   = listOf("日付", "体重")

    private val STAMP = DateTimeFormatter.ofPattern("uuuuMMdd_HHmmss", Locale.ROOT)

    fun fileName(kind: CsvFileKind, at: LocalDateTime): String {
        val prefix = when (kind) {
            CsvFileKind.STRENGTH -> "strength"
            CsvFileKind.CARDIO   -> "cardio"
            CsvFileKind.WEIGHT   -> "weight"
        }
        return "${prefix}_${STAMP.format(at)}.csv"
    }

    fun headerFor(kind: CsvFileKind): List<String> = when (kind) {
        CsvFileKind.STRENGTH -> STRENGTH_HEADER
        CsvFileKind.CARDIO   -> CARDIO_HEADER
        CsvFileKind.WEIGHT   -> WEIGHT_HEADER
    }
}
```

- ファイル名の書式は要件 4.11.1 の `strength_YYYYMMDD_HHMMSS.csv` に一致する。`DateTimeFormatter` のパターンでは `uuuu`（暦年）と `mm`（分）を用いる。`YYYY`（週基準年）と誤らない。
- ロケールは `Locale.ROOT` に固定する。和暦や現地表記が混ざらないようにする。

#### 7.4.1 筋トレファイル

| 列 | 内容 |
|---|---|
| 日付 | `formatDateIso(session.epochDay)` |
| セッション名 | `session.name ?: ""` |
| 種目 | `record.snapshotName` |
| 部位 | `snapshotBodyPart` の表示名 |
| セット数 | 配下セットの件数 |
| セット詳細 | `重量x回数` を `; ` で連結 |

- 対象は `snapshot_category = 'strength'` かつ配下セットが 1 件以上の種目記録（基本設計 3.5、8.5、Q-11）。
- セット詳細の重量は `formatWeight(weightTenthKg)`（M-4）。自重種目は重量を書かず `x10` とする（要件 4.11.2）。
- 並び順は日付昇順 → セッションの `created_at, id` 昇順 → `order_index` 昇順 → セット詳細は `order_index` 昇順（M-12）。ページングのキーがこの順序と一致する。
- 休憩時間・メモ・総ボリュームは出力しない（要件 4.11.2）。

#### 7.4.2 有酸素ファイル

| 列 | 内容 |
|---|---|
| 日付 | `formatDateIso` |
| セッション名 | `session.name ?: ""` |
| 種目 | `record.snapshotName` |
| 実施時間 | `record.durationMin` を整数で |

- 対象は `snapshot_category = 'cardio'` の種目記録すべて。

#### 7.4.3 体重ファイル

| 列 | 内容 |
|---|---|
| 日付 | `formatDateIso` |
| 体重 | `formatBodyWeight(weightTenthKg)`。常に小数第 1 位 |

- 同一日の複数件をすべて登録順に出力する（要件 4.11.4）。日別平均は用いない。

#### 7.4.4 エスケープと改行

```kotlin
object CsvRowBuilder {
    private val NEEDS_QUOTE = charArrayOf(',', '"', '\r', '\n')

    fun escape(value: String): String =
        if (value.any { it in NEEDS_QUOTE }) "\"" + value.replace("\"", "\"\"") + "\""
        else value

    fun row(values: List<String>): String = values.joinToString(",") { escape(it) } + "\r\n"
}
```

- 改行コードは CRLF（Q-6）。最終行にも付ける。
- ヘッダー行も同じ関数を通す。規則を分岐させない。
- BOM はファイル先頭に 1 度だけ書く。行生成側には含めない。
- 対象 0 件でもヘッダー行のみのファイルを 3 つ生成する（M-13）。

### 7.5 MediaStoreGateway と失敗・キャンセル

```kotlin
class MediaStoreGateway @Inject constructor(private val context: Context) {
    fun createPending(displayName: String): Uri
    fun openOutput(uri: Uri): OutputStream
    fun finalize(uri: Uri)
    fun delete(uri: Uri)
    fun resolveUniqueName(baseName: String): String
}
```

登録時の `ContentValues`。

| キー | 値 |
|---|---|
| `DISPLAY_NAME` | `resolveUniqueName` の結果 |
| `MIME_TYPE` | `text/csv` |
| `RELATIVE_PATH` | `Environment.DIRECTORY_DOWNLOADS` |
| `IS_PENDING` | 1 |

- 登録先は `MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)`。ストレージ権限は要求しない（基本設計 8.3、要件 6 章）。
- `resolveUniqueName` は、同名のエントリが存在する場合に `_2`、`_3` と連番を付ける（基本設計 8.6）。MediaStore の自動連番に任せない。実際に付いた名前を利用者へ提示する必要があるため（M-8）。

**キャンセルと失敗の順序**（レビュー指摘に対応。詳細確定）

```
exportOneFile(kind):
    var pendingUri: Uri? = null
    try {
        pendingUri = mediaStore.createPending(name)
        ... 書き込み ...
        mediaStore.finalize(pendingUri)
        pendingUri = null                      -- 確定したので削除対象から外す
        return Success(name, rowCount)
    } catch (c: CancellationException) {
        withContext(NonCancellable) { pendingUri?.let { mediaStore.delete(it) } }
        throw c                                -- 削除を終えてから再スローする
    } catch (t: Throwable) {
        pendingUri?.let { mediaStore.delete(it) }
        return Failed(kind, t.toAppError())
    }
}
```

- `CancellationException` は必ず再スローする。`AppError` へ変換しない。
- 削除は `withContext(NonCancellable)` の中で行う。キャンセル済みのスコープでは通常の中断関数が即座に失敗するため、これがないと未完成ファイルが残る。
- `finalize` に成功したファイルは `pendingUri` を `null` にする。以降のキャンセルで削除されない。既に完了したファイルは残す（基本設計 8.6）。
- キャンセルは 3 ファイルのうち処理中のものだけを削除する。完了済みは残り、未着手は作られない。

**例外系の扱い**

| 事象 | 動作 |
|---|---|
| 開始日が終了日より後 | 実行前の検証でエラー。書き出しを開始しない |
| 同一秒の再実行によるファイル名重複 | `_2`、`_3` の連番を自前で付与 |
| 容量不足、書き込み中の入出力エラー | 当該エントリを削除し `Failed`。他ファイルは続行 |
| MediaStore への登録後、書き込み前に失敗 | 同上 |
| 3 ファイルの一部のみ失敗 | 成功したファイルは残す。結果表示で個別に示す（M-8） |
| 書き込み中のプロセス強制終了 | 削除処理が走らないため未完成ファイルが残る。次回の回収処理で削除する（7.6） |

### 7.6 未完成ファイルの回収

基本設計 8.7 を実装する。レビュー指摘に対応し、選択条件を全文で示す（詳細確定）。

```kotlin
private const val RETENTION_MILLIS = 24L * 60 * 60 * 1000

private val SELECTION = """
    ${MediaStore.Downloads.IS_PENDING} = 1
    AND ${MediaStore.Downloads.DATE_MODIFIED} < ?
    AND (
        ${MediaStore.Downloads.DISPLAY_NAME} LIKE 'strength\_%' ESCAPE '\'
        OR ${MediaStore.Downloads.DISPLAY_NAME} LIKE 'cardio\_%' ESCAPE '\'
        OR ${MediaStore.Downloads.DISPLAY_NAME} LIKE 'weight\_%' ESCAPE '\'
    )
""".trimIndent()

private fun selectionArgs(nowMillis: Long) =
    arrayOf(((nowMillis - RETENTION_MILLIS) / 1000L).toString())
```

- **3 つの `LIKE` を括弧で囲む**。囲まないと `AND` が `OR` より強く結合し、`weight\_%` に一致するだけの新しいファイルまで削除対象に入る。
- `_` は `LIKE` の 1 文字ワイルドカードであるため、`\_` とエスケープし `ESCAPE '\'` を指定する。エスケープしないと `strengthX...` のような名前にも一致する。
- `DATE_MODIFIED` は秒単位である。ミリ秒のしきい値を 1000 で割って比較する。
- 実行契機は `KinnikuApplication.onCreate` と S-10 の画面表示（基本設計 8.7）。実行スコープは `@ApplicationScope`、ディスパッチャは `dispatchers.csv`。
- MediaStore は、他アプリが作成した `IS_PENDING` のエントリを問い合わせ結果に含めない。自アプリが作成したものだけが対象となる。
- 実行中の出力が作るエントリは `DATE_MODIFIED` が現在時刻に近く、しきい値を超えないため削除されない。加えて、出力と回収は `dispatchers.csv` で直列化されるため同時に走らない。
- 回収の成否は利用者へ通知しない。失敗しても出力処理は続行する（基本設計 8.7）。
