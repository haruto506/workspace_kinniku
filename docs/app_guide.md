# 筋トレノート — 利用・ビルド手順

Android 13 以上で使える個人用のオフライン運動記録アプリです。要件定義書 v4.2 と基本設計書 v1.1 に基づいています。

## インストール

`dist/kinniku-1.0.0.apk` を Android 端末へコピーして開き、インストールしてください。端末側で、ファイルを開いたアプリの「不明なアプリのインストール」を許可する必要があります。

USB デバッグが有効な端末へは `adb install -r dist/kinniku-1.0.0.apk` でもインストールできます。ビルド直後のファイルは `app/build/outputs/apk/debug/app-debug.apk` です。

## 使い方

- 今日: 種目を選び、重量・回数を入力して「完了」。自重種目は回数のみ、有酸素は分数のみを入力します。筋トレの完了と同時に休憩タイマーが始まります。
- 同じ種目の続きは「セットを追加」。別のトレーニングは「セッションを追加」で分けられます。
- メニュー: 種目マスタ、テンプレート、期間サマリー、CSV 書き出し、設定。有酸素種目は種目マスタから追加してください。
- カレンダー: 記録のある日を確認します。過去日の記録は閲覧と削除のみです。
- グラフ: 種目 ID ごとの総ボリュームを日別に表示。体重: 日別平均のグラフと個々の記録一覧を切り替えます。過去日の体重も追加できます。
- CSV: ダウンロードへ筋トレ・有酸素・体重の3ファイルを保存。UTF-8 BOM付き、CRLF、期間の両端を含みます。インポートはありません。

未完了の入力は記録ではありません。入力画面を離れると破棄されます。データは端末内だけに保存され、自動バックアップと端末間転送は無効です。アンインストールでデータが消えるため、必要な記録は先に CSV へ書き出してください。

## 開発・ビルド

JDK 17、Android SDK Platform 35、Build Tools 34.0.0 を用意し、`local.properties` の `sdk.dir` に SDK の場所を指定してください。Android Studio ではプロジェクトフォルダを開き、Gradle を同期します。ライブラリの版数は `gradle/libs.versions.toml` で固定しています。

```powershell
./gradlew.bat assembleDebug testDebugUnitTest lintDebug
./gradlew.bat connectedDebugAndroidTest
```

この作業環境では `.tools` に準備した JDK・SDK・Gradle を使えます。

```powershell
./build.ps1
./build.ps1 -Tasks connectedDebugAndroidTest
```

初回ビルドにはライブラリ取得用のインターネット接続が必要です。完成したアプリは全機能をオフラインで使用できます。

個人向けのインストール可能なデバッグ署名 APK を生成します。将来の上書き更新では同じ署名鍵を使ってください。この環境の鍵は `.tools/android-user/debug.keystore` です。鍵を失うと上書き更新できなくなります。鍵と `.tools` は Git 管理対象外です。

## 構成

`app/src/main/java/jp/kinniku/app` 配下に UI / ViewModel、ドメイン規則、Room / Repository、AlarmManager 通知、DataStore 設定、MediaStore CSV を分離しています。Room の初期スキーマは `app/schemas` に出力されます。破壊的マイグレーションは使用しません。

テストは `app/src/test`（入力値・計算・期間・CSV 整形）と `app/src/androidTest`（Room・保存・削除・タイマー・画面操作）にあります。検証結果は `docs/implementation_report.md` を参照してください。
