package jp.kinniku.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File

@RunWith(AndroidJUnit4::class)
class UiSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun recordAndNavigate() {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("種目を追加").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("種目を追加").onFirst().performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("ベンチプレス").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("ベンチプレス").performClick()
        compose.onNodeWithText("重量 (kg)").performScrollTo().performTextInput("60")
        compose.onNodeWithText("回数").performScrollTo().performTextInput("10")
        compose.onNodeWithText("完了").performScrollTo().performClick()
        compose.waitUntil(15_000) {
            compose.onAllNodesWithText("今はしない").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("60 kg × 10 回").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("今はしない").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("今はしない").performClick()
        compose.onNodeWithText("60 kg × 10 回").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("リセット").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithContentDescription("リセット").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("今日の積み重ね").performScrollTo()
        screenshot("today")
        compose.onNode(hasText("体重") and hasClickAction()).performClick()
        compose.onNodeWithText("体重を記録").performClick()
        compose.onNodeWithText("体重 (kg)").performTextInput("60.5")
        compose.onNodeWithText("保存").performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("体重 (kg)").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("一覧").performClick()
        compose.onNodeWithText("60.5 kg").assertExists()
        screenshot("weight")
        compose.onNodeWithText("今日").performClick()
        compose.onNodeWithContentDescription("メニュー").performClick()
        compose.onNodeWithText("CSV 書き出し").performClick()
        compose.onNodeWithText("CSV を書き出す").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("保存済み:", substring = true).fetchSemanticsNodes().size == 3 }
        compose.onAllNodesWithText("保存済み:", substring = true).assertCountEquals(3)
        screenshot("csv")
    }
}
