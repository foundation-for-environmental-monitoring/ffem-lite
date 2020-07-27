package io.ffem.lite.external

import android.content.Context
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.uiautomator.By
import io.ffem.lite.R
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.clickLaunchButton
import io.ffem.lite.common.TestHelper.gotoFormSubmit
import io.ffem.lite.common.TestHelper.gotoSurveyForm
import io.ffem.lite.common.TestHelper.gotoSurveySelection
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestHelper.nextSurveyPage
import io.ffem.lite.common.TestHelper.selectMenuItem
import io.ffem.lite.common.TestHelper.startSurveyApp
import io.ffem.lite.common.TestHelper.takeScreenshot
import io.ffem.lite.common.TestUtil
import io.ffem.lite.common.pH
import io.ffem.lite.common.testDataList
import io.ffem.lite.internal.TIME_DELAY
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.toLocalString
import io.ffem.lite.model.toResourceId
import io.ffem.lite.ui.ResultListActivity
import io.ffem.lite.util.PreferencesUtil
import io.ffem.lite.util.toLocalString
import org.hamcrest.Matchers
import org.junit.Assert

fun startTest(imageNumber: Int) {
    val testData = testDataList[imageNumber]!!
    val screenshotName = testData.testDetails.id
    val context = ApplicationProvider.getApplicationContext<Context>()

    TestHelper.screenshotCount = -1

    PreferencesUtil.setString(
        ApplicationProvider.getApplicationContext(),
        R.string.testImageNumberKey, imageNumber.toString()
    )

    SystemClock.sleep(1000)

    startSurveyApp()

    SystemClock.sleep(7000)

    takeScreenshot(screenshotName)

    gotoSurveySelection()

    takeScreenshot(screenshotName)

    gotoSurveyForm()

    nextSurveyPage(3, context.getString(testData.testDetails.group))

    takeScreenshot(screenshotName)

    clickLaunchButton(testData.testDetails.buttonIndex)

    SystemClock.sleep(1000)

    takeScreenshot(screenshotName)

    if (testData.expectedScanError == -1) {

        SystemClock.sleep(TIME_DELAY)

        Espresso.onView(ViewMatchers.withText(testData.testDetails.name.toLocalString()))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        if (testData.expectedResultError > ErrorType.NO_ERROR) {
            Espresso.onView(
                ViewMatchers.withText(
                    testData.expectedResultError.toLocalString(
                        context
                    )
                )
            ).check(
                ViewAssertions.matches(ViewMatchers.isDisplayed())
            )

            takeScreenshot(screenshotName)

            Espresso.onView(ViewMatchers.withText(R.string.close)).perform(ViewActions.click())

            SystemClock.sleep(1000)

            takeScreenshot(screenshotName)

            SystemClock.sleep(1000)

            launchActivity<ResultListActivity>()
            SystemClock.sleep(1000)

            takeScreenshot(screenshotName)

        } else {

            Espresso.onView(ViewMatchers.withText(testData.testDetails.name.toLocalString()))
                .check(
                    ViewAssertions.matches(
                        ViewMatchers.isDisplayed()
                    )
                )

            val resultTextView = Espresso.onView(ViewMatchers.withId(R.id.text_result))

            resultTextView.check(ViewAssertions.matches(TestUtil.checkResult(testData.expectedResult)))
            val convertedValue = TestUtil.getText(resultTextView).toDouble()

            if (testData.testDetails == pH) {
                Espresso.onView(ViewMatchers.withId(R.id.text_unit))
                    .check(ViewAssertions.matches(Matchers.not(ViewMatchers.isDisplayed())))
            } else {
                Espresso.onView(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.text_unit),
                        ViewMatchers.withText("mg/l")
                    )
                ).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            }

            val marginOfErrorView = Espresso.onView(ViewMatchers.withId(R.id.text_error_margin))
            marginOfErrorView.check(ViewAssertions.matches(TestUtil.checkResult(testData.expectedMarginOfError)))

            Espresso.onView(
                ViewMatchers.withText(
                    testData.risk.toResourceId(
                        ApplicationProvider.getApplicationContext(), testData.testDetails.riskType
                    )
                )
            ).check(
                ViewAssertions.matches(ViewMatchers.isDisplayed())
            )

            takeScreenshot(screenshotName)

            Espresso.onView(ViewMatchers.withText(R.string.submit_result))
                .perform(ViewActions.click())

            SystemClock.sleep(1000)

            takeScreenshot(screenshotName)

            Assert.assertNotNull(mDevice.findObject(By.text(convertedValue.toString())))

            selectMenuItem()

            SystemClock.sleep(1000)

            takeScreenshot(screenshotName)

            SystemClock.sleep(1000)

            gotoFormSubmit()

            SystemClock.sleep(1000)

            takeScreenshot(screenshotName)

            SystemClock.sleep(1000)

            launchActivity<ResultListActivity>()
            SystemClock.sleep(1000)

            takeScreenshot(screenshotName)
        }

    } else {

        SystemClock.sleep(TIME_DELAY)

        Espresso.onView(ViewMatchers.withText(testData.expectedScanError))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}