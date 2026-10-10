<!-- reviewed-source-sha256: 95ee627fc03b00072fdb9976958b4a20b9f26301f2649c00ef37ce3c9dc36557 -->
# 詳細設計書レビュー結果

- レビュー対象: `docs/detailed_design.md` v0.2（ドラフト）
- 参照文書: `docs/requirements.md` v4.2、`docs/basic_design.md` v1.1
- レビュー日時: 2026-09-06（Asia/Tokyo）
- 総合判定: **実装不可**

## 結論

種目スナップショットの再取得、Flow 集計、通知状態の分離、CSV キャンセル、世代指定アラームは改善された。一方で、参照基準・ビルド定義・UI 契約・CSV transaction 契約が未完であり、タイマー引継ぎの競合も残る。実装者が推測なしで実装・試験できる状態ではない。

件数: P0 5件、P1 8件、P2 3件。

## 既存指摘の状態

| ID | 状態 | 現在の扱い |
|---|---|---|
| R-01 | 一部解決 | M/Q の凍結規則は追加されたが、根拠として指す 14〜17章が存在せず、参照版も現行上位文書と不一致。 |
| R-02 | 未解決 | package、Gradle、完全依存版数、Manifest の完成契約がない。 |
| R-03 | 一部解決 | DAO/Repository は増えたが、CSV transaction を含む全契約は閉じていない。 |
| R-04 | 未解決 | 全11画面の UiState/Event/Effect が揃っていない。 |
| R-05 | 解決 | 種目IDから transaction 内でマスタを再取得して snapshot を作る。 |
| R-06 | 一部解決 | 検証規則は追加されたが、全更新の transaction 所有者が未確定。 |
| R-07 | 一部解決 | recordId 引継ぎは定義済みだが、commit後・SavedState反映前の終了時の扱いがない。 |
| R-08 | 解決 | Flow と体重変更信号を combine する。 |
| R-09 | 一部解決 | 世代指定 PendingIntent は導入済みだが、commit と handover が競合する。 |
| R-10 | 解決 | BOOT_COUNT_UNKNOWN を再起動扱いにする。 |
| R-11 | 解決 | 直接 APK 配布、USE_EXACT_ALARM、フォールバックを整合した。 |
| R-12 | 未解決 | 空集合の totalForDay と DTO nullability が不整合。 |
| R-13 | 一部解決 | 有酸素のみを除外したが、0セット筋トレと順序が未解決。 |
| R-14 | 解決 | 整数刻みにより重複軸ラベルを防ぐ。 |
| R-15 | 解決 | template の sessionId と結果型で適用先を渡す。 |
| R-16 | 一部解決 | 未来日不可は定めたが、S-08 の表示範囲契約がない。 |
| R-17 | 未解決 | CSV の保持単位が基本設計と矛盾する。 |
| R-18 | 一部解決 | 回収 selection は改善したが、同名 insert 競合時の契約がない。 |
| R-19 | 解決 | LoadResult が失敗と最後の正常値を持つ。 |
| R-20 | 解決 | Application と Activity の責務・Scope を分離した。 |
| R-21 | 解決 | 日付変更を購読し保存前にも再判定する。 |
| R-22 | 解決 | 通知権限・アプリ通知・チャンネルを分離した。 |
| R-23 | 解決 | NonCancellable で pending URI を削除する。 |
| R-24 | 解決 | エンティティ8件、DAO11件と AppDatabase が一致する。 |
| R-25 | 一部解決 | RestTimer の例以外は Room transaction 方式が閉じていない。 |
| R-26 | 一部解決 | Channel の説明は訂正済みだが Effect 契約が不足。 |
| R-27 | 一部解決 | Locale は固定したが共有 DecimalFormat の安全性が未定義。 |
| R-28 | 一部解決 | NameKey は明記したが上位文書との差分追跡がない。 |
| R-29 | 解決 | 主要補助型・projection DTO は定義済み。 |
| R-30 | 未解決 | 本書が宣言するテストケース一覧（12章）が存在しない。 |

## 指摘

### R-01 — 参照基準と決定事項の追跡先が存在しない

- 優先度: P0
- 状態: 一部解決
- 該当章: 0.1、0.4、0.5、0.6、1.4、2.4.5、4.5、4.6、6.3、6.5
- 問題: 本書は要件 v4.1／基本設計 v1.0 を対象とするが、現行参照文書は v4.2／v1.1 である。さらに14〜17章、12.1/12.2、10.3、付録A、D-1〜D-8、改訂提案R-1〜R-7を根拠にするが、本書は7.6で終了する。0.5は上位と異なる場合に本書を優先させるが、差分一覧が存在しない。
- 実装上の影響: NameKey、グラフ順、CSV方式、Manifest・テスト要求の正が上位文書と本文のどちらかを実装者が判断することになる。
- 具体的な修正案: メタデータを現行版へ更新し、決定事項、上位との差分、対応表、テスト章を実在する章として追加する。差分は内容・優先順位・反映先を表で閉じる。
- 完了条件: 全章参照が解決し、現行上位文書との差分を1件ずつ追跡できる。

### R-02 — ビルド・Manifest 構成が実装可能な粒度でない

- 優先度: P0
- 状態: 未解決
- 該当章: 0.2、1.2、2.5、5.6、6.4
- 問題: `<package>` のままで、namespace/applicationId、versionCode/versionName、Gradle wrapper/AGP/Kotlin/KSP/Compose BOM/Room/Hilt/Navigation/DataStore の完全版数、KSP・Hilt・Parcelize・schema 出力設定がない。USE_EXACT_ALARM、POST_NOTIFICATIONS、Application/Activity/Receiver、allowBackup、dataExtractionRules、アプリ名・アイコンの完成した Manifest/リソース契約もない。
- 実装上の影響: 同期・ビルド、権限、バックアップ遮断、Receiver 到達性を実装者が選定・推測することになる。
- 具体的な修正案: libs.versions.toml、root/app Gradle、Manifest、backup/data-extraction XML、strings/iconについて識別子・完全版数・必須属性を完成形または同等の表で定義する。
- 完了条件: 追加の選定なしに Gradle 同期、debug APK ビルド、Room schema 出力ができる。

### R-03 — DAO／Repository の契約がまだ実装不能な箇所を持つ

- 優先度: P0
- 状態: 一部解決
- 該当章: 2.4.10、3.1〜3.12、7.2〜7.4
- 問題: `CsvDao.strengthPage()` は @Transaction の抽象宣言だけで、session page→records→sets→CsvPage の構築、空リスト分岐、次キーを定義しない。CsvPageにも @Relation 等の生成契約がない。また「自分自身を除く name_key 重複検索」、createSession の日付・名称検証、template更新の対象不存在判定にも公開契約がない。
- 実装上の影響: CSV はコンパイル不能または実装ごとにページ取得が分岐し、更新の TargetGone 境界も変わる。
- 具体的な修正案: concrete DAO transaction または AppDatabase.withTransaction の実装可能な疑似コードでページ構築を定義し、不足 query と Repository の戻り値・transaction 所有者を追加する。
- 完了条件: 全イベントとCSV各ページを定義済みAPIだけで実装でき、0件・削除競合の戻り値が一意になる。

### R-04 — 11画面の UI 公開契約が揃っていない

- 優先度: P0
- 状態: 未解決
- 該当章: 6.1〜6.3
- 問題: UiState のコード定義はS-01/S-02/S-07/S-10のみで、sealed Event/Effect は全画面で未定義。S-03〜S-06、S-08、S-09、S-11には編集中ID、入力、確認ダイアログ、検証エラー、TargetGone時の遷移、OS設定・戻る効果がない。
- 実装上の影響: ViewModel API、Compose状態、ダイアログ再作成、Effect配送を実装者が設計し、画面ごとに挙動が分岐する。
- 具体的な修正案: S-01〜S-11それぞれにUiState全フィールド・初期値、Event、Effect、ダイアログ状態、成功/失敗遷移、結果キー、アクセシビリティ状態を定義する。
- 完了条件: ViewModel/Composableの公開APIを追加設計せず実装でき、主要UIテストの入力・期待Effectを文書から作れる。

### R-09 — タイマー引継ぎが古い世代のアラームを再登録できる

- 優先度: P0
- 状態: 一部解決
- 該当章: 3.5、4.8、5.3、5.5.2
- 問題: セット確定でRepositoryがDB世代を作った後、別呼び出しのapplyHandoverがmutex内でOS操作する。 `commit A → commit B → handover B → handover A` と交差すると、Aが置換済みtokenを後からscheduleし、last_alarm_*も古い世代へ戻り得る。
- 実装上の影響: 古いアラームが残り、プロセス復元時の取り残し予約回収がDBの現行世代を指さない。
- 具体的な修正案: RecordRepository呼び出しからOS handoverまでをCoordinatorの同一mutex内に置く。代替案ではmutex内で現行rest_timerのtoken/targetSetIdを再照合し、不一致handoverはscheduleせず捨てる。
- 完了条件: 連続セット確定、完了受信、削除、復元を交差させても、DB現行tokenの予約だけが残りlast_alarm_*も同じ世代を示す。

### R-06 — 全更新系の transaction 所有者と検証が閉じていない

- 優先度: P1
- 状態: 一部解決
- 該当章: 3.1〜3.3、3.7〜3.9、3.12
- 問題: 全更新をtransaction内で検証するとしつつ、Repositoryの@TransactionはRoomが有効にする位置ではない。createSession/template/種目更新のtransaction所有者と対象不存在分岐、RestTimerTxDaoのAppDatabase/DI上の位置が未定義。
- 実装上の影響: 検証後の競合削除、存在しないIDへの成功扱い、部分コミットが実装ごとに発生する。
- 具体的な修正案: 更新ごとにAppDatabase.withTransactionかDAO concrete transactionを指定し、取得→検証→更新→0件判定を固定する。
- 完了条件: 全更新操作のtransaction実装場所、DAO、競合/0件戻り値を追跡できる。

### R-07 — 下書きの commit 後クラッシュ時の冪等性が未定義

- 優先度: P1
- 状態: 一部解決
- 該当章: 3.4、6.3.1
- 問題: 初回commit成功後・recordIdをSavedStateへ反映する前の終了では、復元した下書きがrecordId=nullのまま再確定できる。この既存セットの検出、重複許容、利用者への扱いがない。
- 実装上の影響: 同じ入力の二重セット・二重種目記録が偶発的に作られ、試験期待値も定まらない。
- 具体的な修正案: 重複を許容するならその結果を明記して試験する。許容しないならclient operation IDを永続化し初回commitを冪等化する。
- 完了条件: commit前、commit後SavedState前、保存後の終了試験に期待DB状態と再操作結果がある。

### R-12 — totalForDay は空集合を nullable で受けられない

- 優先度: P1
- 状態: 未解決
- 該当章: 2.3、2.4.7、3.10
- 問題: GROUP BYなしのtotalForDayは0件でもSUM=NULL、COUNT=0の1行を返すが、DailyWeightTotal.sumTenthは非nullable Longで、本文はnullまたはcount==0を前提にする。
- 実装上の影響: Roomのnullマッピング例外、または「直近体重なし」と0の混同により自重総ボリュームが誤る。
- 具体的な修正案: GROUP BY epoch_dayを追加して0件をnullにする。代替はsumTenthをnullableにしてcount=0処理を明記する。
- 完了条件: 0件、1件、同日複数件のDAO試験でnull/合計/件数と体重解決結果が一致する。

### R-13 — グラフ選択肢に空グラフが残り、並びも決定と矛盾する

- 優先度: P1
- 状態: 一部解決
- 該当章: 2.4.1、6.3.7
- 問題: observeGraphSelectableは筋トレexercise_recordだけで選ぶため、0セット記録の種目を返す。またID昇順だが、本文が参照するD-4と前回レビュー決定は累計使用回数降順・ID昇順であり、その決定表は存在しない。
- 実装上の影響: 空グラフを選ばせるほか、初期選択とUI試験が実装ごとに変わる。
- 具体的な修正案: EXISTSにworkout_setを結合し1件以上を条件にする。削除済みを含む使用回数と並び順を上位と整合する決定表で固定する。
- 完了条件: 有酸素のみ、0セット、通常、削除済み、同率使用回数のDAO試験がある。

### R-17 — CSV のメモリ上限が基本設計と矛盾する

- 優先度: P1
- 状態: 未解決
- 該当章: 2.4.10、7.3、基本設計8.6
- 問題: 本書は50セッション、最大400種目記録・2,000セットを保持するが、基本設計は種目記録1件分だけを保持して1行ずつ書く。本書が参照する17章の改訂提案は存在しない。
- 実装上の影響: DAO、メモリ量、最大件数試験がページ保持か種目記録ストリーミングかで分岐する。
- 具体的な修正案: 両文書の方式を統一する。ページ保持を採るなら上位改訂と最大メモリ見積りを、種目記録保持を採るならキーとDAO変更を明記する。
- 完了条件: 3文書で同時保持件数が一致し、最大件数のメモリ/時間受入試験がある。

### R-18 — MediaStore 同名解決の原子性と検索契約がない

- 優先度: P1
- 状態: 一部解決
- 該当章: 7.5、7.6
- 問題: selectionは改善されたが、resolveUniqueNameのprojection/検索条件、名前決定からinsertまでの競合時の再試行回数がない。
- 実装上の影響: 結果画面の名前と実際のMediaStore名がずれる、または同一秒出力で失敗する実装が生じる。
- 具体的な修正案: Downloadsのprojection、完全一致条件、base→_2→_3の探索、insert失敗時の次番号再試行上限と失敗結果を定義する。
- 完了条件: 同一秒連続、既存_2、insert競合で確定displayNameと結果表示が一致する。

### R-25 — Room transaction の実装方式を全操作で確定する

- 優先度: P1
- 状態: 一部解決
- 該当章: 2.4.10、3.5〜3.9、3.12
- 問題: @Transaction表記だけではRepository処理はRoom transactionにならない。RestTimer例以外はDAO concrete methodかwithTransactionか未定義で、strengthPageも抽象宣言である。
- 実装上の影響: セット確定、削除＋再採番、種目変更＋テンプレート波及、CSV取得が部分コミットされ得る。
- 具体的な修正案: 主要処理ごとにtransaction ownerとコンパイル可能なRoomシグネチャを示し、rollback例外とAppResult変換位置を統一する。
- 完了条件: 主要処理ごとに途中例外で全更新がrollbackされるRoom試験がある。

### R-30 — 受入条件・回帰テストが存在しない

- 優先度: P1
- 状態: 未解決
- 該当章: 0.2、2.6、5章、6章、7章
- 問題: 0.2はテストケース一覧をスコープに含めるが、本書は7.6で終わり、12章を参照するだけである。正常・境界・競合・外部失敗時の前提、操作、期待DB/UI/OS作用がない。
- 実装上の影響: P0/P1の完了を客観判定できず、タイマー競合、空集合集計、CSV回収、権限、日付境界の回帰を見落とす。
- 具体的な修正案: テスト章を追加し、要件・R-01〜R-30ごとに対象層、前提、操作、期待値を追跡する。
- 完了条件: 各必須仕様にテストIDと受入条件があり、第三者が合否判定できる。

### R-26 — Effect の非表示中配送規則が検証できない

- 優先度: P2
- 状態: 一部解決
- 該当章: 6.1.1、6.3
- 問題: Channel.BUFFEREDの説明は正しいが、非表示中のSnackbar/Navigation/権限要求を復帰後に配送するか破棄するかが未定義。
- 実装上の影響: 古い遷移・権限ダイアログを再実行する実装と失う実装に分岐する。
- 具体的な修正案: 全Effectに再配送可否、SavedStateでの重複抑止、破棄条件を定義する。
- 完了条件: lifecycle停止中に各Effectを発行するUI試験で期待動作を確認できる。

### R-27 — DecimalFormat の共有利用が安全でない

- 優先度: P2
- 状態: 一部解決
- 該当章: 6.5
- 問題: top-level singletonのDecimalFormatはスレッドセーフでなく、CSVとUIが別dispatcherから共有するか未定義。
- 実装上の影響: 並行利用時に表示値が壊れる実装を許す。
- 具体的な修正案: 呼び出しごとに生成するか、共有しない整数文字列変換に変更する。
- 完了条件: 所有スレッドまたは並行安全な方式が明記され、並列試験を通る。

### R-28 — NameKey の上位文書との差分が未追跡

- 優先度: P2
- 状態: 一部解決
- 該当章: 4.5、基本設計7.2
- 問題: 本書はNFKCを先に行うが、基本設計の順序と異なる。0.1が参照する17章の改訂提案は存在しない。
- 実装上の影響: 初期データ、重複検査、将来migrationで異なるkeyを作る余地が残る。
- 具体的な修正案: 上位文書を同じ順序へ改訂するか、優先順位と既存データ影響を差分表へ記録し、Unicode空白の期待値表を追加する。
- 完了条件: 3文書の変換順が一致し、同一入力集合の期待keyを共有試験で確認できる。

## 別エージェントへ渡せる状態

- [ ] 現行上位文書と対応し、全参照章・決定表・差分表が実在する
- [ ] package、Gradle、完全版数、Manifest、バックアップ設定を追加選定なしで実装できる
- [ ] 全DAO/Repository/Room transactionのコンパイル可能な契約がある
- [ ] 全11画面のUiState/Event/Effect、入力・確認・失敗遷移・Navigationが定義されている
- [ ] タイマーcommitとOSアラーム引継ぎが同一競合規則で直列化されている
- [ ] commit後クラッシュ時の下書き再操作が一意である
- [ ] 空集合体重、グラフ選択肢、CSVメモリ上限・同名競合が上位文書と整合する
- [ ] P0/P1全項目に正常・境界・競合・失敗の受入テストがある

現時点では未達であり、別エージェントへ実装を委任できない。
