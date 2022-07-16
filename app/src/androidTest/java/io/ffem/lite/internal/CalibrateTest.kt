package io.ffem.lite.internal


import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.common.TEST_SURVEY_NAME
import io.ffem.lite.common.TestHelper
import io.ffem.lite.common.TestHelper.clearPreferences
import io.ffem.lite.common.TestHelper.isDeviceInitialized
import io.ffem.lite.common.TestHelper.mDevice
import io.ffem.lite.common.TestUtil.childAtPosition
import io.ffem.lite.common.TestUtil.getBackgroundColor
import io.ffem.lite.common.TestUtil.hasBackgroundColor
import io.ffem.lite.common.TestUtil.sleep
import io.ffem.lite.common.TestUtil.withIndex
import io.ffem.lite.common.getString
import io.ffem.lite.data.clearData
import io.ffem.lite.ui.ResultListActivity
import org.hamcrest.Matchers.allOf
import org.junit.*
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class CalibrateTest {

    @get:Rule
    val mActivityTestRule = activityScenarioRule<ResultListActivity>()

    @Before
    fun setUp() {
        clearPreferences()
    }

    @Test
    fun calibrateTest() {
        TestHelper.startDiagnosticMode()
        sleep(200)

        pressBack()

        sleep(200)

        onView(withId(R.id.scrollViewSettings)).perform(swipeUp())

        sleep(200)

        onView(withText("Return dummy results")).perform(click())

        sleep(200)

        onView(withId(R.id.scrollViewSettings)).perform(swipeDown())

        sleep(200)

        onView((withText(R.string.calibrate))).perform(click())

        onView((withText(R.string.water))).perform(click())

        try {
//            sleep(500)
//            onView(withId(R.id.tests_lst)).perform(swipeUp())
//            sleep(500)
            onView((withText("Fluoride"))).perform(click())
        } catch (e: Exception) {

            val recyclerView2 = onView(
                allOf(
                    withId(R.id.tests_lst),
                )
            )
            recyclerView2.perform(actionOnItemAtPosition<ViewHolder>(7, click()))
        }

        sleep(1000)

        onView(withId(R.id.calibration_lst))
            .perform(actionOnItemAtPosition<ViewHolder>(0, click()))

        sleep(200)

        try {
            onView(
                allOf(
                    withId(R.id.editExpiryDate),
                    isDisplayed()
                )
            ).perform(click())

            onView(
                allOf(
                    withId(android.R.id.button1), withText(android.R.string.ok),
                    isDisplayed()
                )
            ).perform(click())

            onView(
                allOf(
                    withId(android.R.id.button1), withText(R.string.save),
                    isDisplayed()
                )
            ).perform(click())

            sleep(200)
        } catch (e: Exception) {
        }

        sleep(1000)

//        for (i in 0..4) {
//            onView(withText(R.string.next)).perform(click())
//            sleep(200)
//        }
//
//        onView((withText(R.string.skip))).perform(click())
//
//        sleep(500)
//
//        onView(withText(R.string.next)).perform(click())

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(200)

        onView(
            allOf(
                withId(R.id.next_txt), withText(R.string.next),
                childAtPosition(
                    allOf(
                        withId(R.id.footer_lyt),
                    ),
                    4
                ),
                isDisplayed()
            )
        ).perform(click())

        onView(
            allOf(
                withId(R.id.accept_button), withText(R.string.done),
                isDisplayed()
            )
        ).perform(click())

        sleep(1000)

        onView(withId(R.id.calibration_lst))
            .perform(actionOnItemAtPosition<ViewHolder>(1, click()))

        sleep(1000)

//        for (i in 0..4) {
//            onView(withText(R.string.next)).perform(click())
//            sleep(200)
//        }
//
//        onView((withText(R.string.skip))).perform(click())
//
//        sleep(500)
//
//        onView(withText(R.string.next)).perform(click())

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(200)

        onView(
            allOf(
                withId(R.id.next_txt),
                withText(R.string.next),
                withContentDescription(R.string.next),
                childAtPosition(
                    allOf(
                        withId(R.id.footer_lyt),
                    ),
                    4
                ),
                isDisplayed()
            )
        ).perform(click())

        onView(withText(R.string.done)).perform(click())

        sleep(1000)

        onView(withId(R.id.calibration_lst)).perform(actionOnItemAtPosition<ViewHolder>(2, click()))

        sleep(1000)

//        for (i in 0..4) {
//            onView(withText(R.string.next)).perform(click())
//            sleep(200)
//        }
//
//        onView((withText(R.string.skip))).perform(click())
//
//        sleep(500)
//
//        onView(withText(R.string.next)).perform(click())

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(200)

        val view = onView(
            allOf(
                withId(R.id.color_vue),
                withParent(withParent(withId(R.id.result_lyt))),
                isDisplayed()
            )
        )
        view.check(matches(isDisplayed()))

//        view.check(matches(hasBackgroundColor(R.color.red_500)))

        val backgroundColor = getBackgroundColor(
            allOf(
                withId(R.id.color_vue),
                withParent(withParent(withId(R.id.result_lyt)))
            )
        )

        val textView = onView(
            allOf(
                withId(R.id.value_txt), withText("1.0"),
                withParent(withParent(withId(R.id.result_lyt))),
                isDisplayed()
            )
        )
        textView.check(matches(withText("1.0")))

        val textView2 = onView(
            allOf(
                withId(R.id.name_txt), withText(R.string.fluoride),
                withParent(withParent(withId(R.id.result_lyt))),
                isDisplayed()
            )
        )
        textView2.check(matches(withText(R.string.fluoride)))

        onView(
            allOf(
                withId(R.id.next_txt),
                withText(R.string.next),
                withContentDescription(R.string.next),
                childAtPosition(
                    allOf(
                        withId(R.id.footer_lyt),
                    ),
                    4
                ),
                isDisplayed()
            )
        ).perform(click())
        onView(withText(R.string.done)).perform(click())

        sleep(1000)

        onView(withId(R.id.calibration_lst))
            .perform(actionOnItemAtPosition<ViewHolder>(3, click()))

        sleep(1000)

//        for (i in 0..4) {
//            onView(withText(R.string.next)).perform(click())
//            sleep(200)
//        }
//
//        onView((withText(R.string.skip))).perform(click())
//
//        sleep(500)
//
//        onView(withText(R.string.next)).perform(click())

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(200)

        onView(
            allOf(
                withId(R.id.next_txt),
                withText(R.string.next),
                withContentDescription(R.string.next),
                childAtPosition(
                    allOf(
                        withId(R.id.footer_lyt),
                    ),
                    4
                ),
                isDisplayed()
            )
        ).perform(click())
        onView(withText(R.string.done)).perform(click())

        sleep(1000)

        onView(withId(R.id.calibration_lst))
            .perform(actionOnItemAtPosition<ViewHolder>(4, click()))

        sleep(1000)

//        for (i in 0..4) {
//            onView(withText(R.string.next)).perform(click())
//            sleep(200)
//        }
//
//        onView((withText(R.string.skip))).perform(click())
//
//        sleep(500)

//        onView(withText(R.string.next)).perform(click())

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(200)

        onView(
            allOf(
                withId(R.id.next_txt),
                withText(R.string.next),
                withContentDescription(R.string.next),
                childAtPosition(
                    allOf(
                        withId(R.id.footer_lyt),
                    ),
                    4
                ),
                isDisplayed()
            )
        ).perform(click())
        sleep(1000)
        onView(withText(R.string.done)).perform(click())

        sleep(200)

        val textView3 = onView(
            allOf(
                withId(R.id.value_txt), withText("1.00"),
                withParent(
                    allOf(
                        withId(R.id.layout_row),
                        withParent(withId(R.id.calibration_lst))
                    )
                ),
                isDisplayed()
            )
        )
        textView3.check(matches(withText("1.00")))

        sleep(200)

        onView(
            allOf(
                withIndex(withId(R.id.swatch_view), 2),
                hasBackgroundColor(backgroundColor)
            )
        ).check(matches(isDisplayed()))

        pressBack()

        pressBack()

        pressBack()

        sleep(500)

        onView(withId(R.id.start_test_fab)).perform(click())

        sleep(3000)
        mDevice.findObject(By.text(getString(R.string.enter_data))).click()

        sleep(1000)

        try {
            mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        } catch (e: Exception) {
            swipeUp()
            mDevice.findObject(By.text(TEST_SURVEY_NAME)).click()
        }
        sleep(2000)

        mDevice.findObject(By.text(getString(R.string.next).uppercase())).click()

        sleep(500)

        mDevice.findObject(By.text(getString(R.string.fluoride))).click()

        sleep(500)

        onView(withId(R.id.noDilution_btn)).perform(click())

        sleep(1000)

        onView((withText("Generate Dummy Result"))).perform(click())

        sleep(500)

        onView(
            allOf(
                withId(R.id.next_txt),
                withText(R.string.next),
                withContentDescription(R.string.next),
                childAtPosition(
                    allOf(
                        withId(R.id.footer_lyt),
                    ),
                    4
                ),
                isDisplayed()
            )
        ).perform(click())

        sleep(2000)

        onView(withText(R.string.done)).perform(click())

        onView(allOf(withText(R.string.fluoride), isDisplayed()))
    }

    companion object {
        @JvmStatic
        var initialized = false

        @JvmStatic
        @BeforeClass
        fun initialize() {
            BuildConfig.USE_SCREEN_PINNING.set(false)
            if (!isDeviceInitialized()) {
                mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            }
        }

        @JvmStatic
        @AfterClass
        fun teardown() {
            clearData(ApplicationProvider.getApplicationContext())
        }
    }
}
