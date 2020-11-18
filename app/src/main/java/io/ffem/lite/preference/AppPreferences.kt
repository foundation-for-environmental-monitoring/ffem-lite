package io.ffem.lite.preference

import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.IMAGE_FILE_NAME
import io.ffem.lite.app.App.Companion.IS_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_CALIBRATION
import io.ffem.lite.util.MAX_COLOR_DISTANCE_RGB
import io.ffem.lite.util.PreferencesUtil
import java.util.*
import java.util.concurrent.TimeUnit

fun getSampleTestImageNumber(): Int {
    var testImageNumber = -1
    if (isTestRunning()) {
        testImageNumber = try {
            PreferencesUtil
                .getString(App.app, R.string.testImageNumberKey, " -1").toInt()
        } catch (e: Exception) {
            -1
        }
    }
    return testImageNumber
}

fun getSampleTestImageNumberInt(): Int {
    return try {
        getSampleTestImageNumber()
    } catch (e: Exception) {
        -1
    }
}

fun isTestRunning(): Boolean {
    return BuildConfig.DEBUG && (isDiagnosticMode() || BuildConfig.INSTRUMENTED_TEST_RUNNING.get())
}

fun isDiagnosticMode(): Boolean {
    return PreferencesUtil.getBoolean(App.app, R.string.diagnosticModeKey, false)
}

fun useFlashMode(): Boolean {
    return isDiagnosticMode() && PreferencesUtil.getBoolean(
        App.app,
        R.string.useFlashModeKey,
        false
    )
}

fun useDummyImage(): Boolean {
    return isDiagnosticMode() && PreferencesUtil.getBoolean(
        App.app,
        R.string.dummyImageKey, false
    )
}

fun manualCaptureOnly(): Boolean {
    return PreferencesUtil.getBoolean(
        App.app,
        R.string.manualCaptureOnlyKey, false
    )
}

fun getColorDistanceTolerance(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.colorDistanceToleranceKey,
                MAX_COLOR_DISTANCE_RGB.toString()
            )
        )
    } else {
        MAX_COLOR_DISTANCE_RGB
    }
}

fun getCalibrationColorDistanceTolerance(): Int {
    return if (isDiagnosticMode()) {
        Integer.parseInt(
            PreferencesUtil.getString(
                App.app,
                R.string.maxCardColorDistanceAllowedKey,
                MAX_COLOR_DISTANCE_CALIBRATION.toString()
            )
        )
    } else {
        MAX_COLOR_DISTANCE_CALIBRATION
    }
}

object AppPreferences {

    fun enableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, true)
        PreferencesUtil.setLong(
            App.app, R.string.diagnosticEnableTimeKey,
            Calendar.getInstance().timeInMillis
        )
    }

    fun disableDiagnosticMode() {
        PreferencesUtil.setBoolean(App.app, R.string.diagnosticModeKey, false)
        PreferencesUtil.setBoolean(App.app, R.string.testModeOnKey, false)
        PreferencesUtil.removeKey(App.app, R.string.testImageNumberKey)
    }

    fun checkDiagnosticModeExpiry() {
        if (isDiagnosticMode()) {
            val lastCheck = PreferencesUtil.getLong(App.app, R.string.diagnosticEnableTimeKey)
            if (TimeUnit.MILLISECONDS.toMinutes(Calendar.getInstance().timeInMillis - lastCheck) > 20) {
                disableDiagnosticMode()
            } else {
                PreferencesUtil.setLong(
                    App.app, R.string.diagnosticEnableTimeKey,
                    Calendar.getInstance().timeInMillis
                )
            }
        }
    }

    fun isCalibration(): Boolean {
        return PreferencesUtil.getBoolean(App.app, IS_CALIBRATION, false)
    }

    fun generateImageFileName() {
        PreferencesUtil.setString(App.app, IMAGE_FILE_NAME, UUID.randomUUID().toString())
    }

    fun getImageFilename(): String {
        return PreferencesUtil.getString(App.app, IMAGE_FILE_NAME, UUID.randomUUID().toString())!!
    }

//    fun isAppUpdateCheckRequired(): Boolean {
//        if (BuildConfig.INSTRUMENTED_TEST_RUNNING.get()) {
//            return true
//        }
//        val lastCheck = PreferencesUtil.getLong(App.app, "lastUpdateCheck")
//        return TimeUnit.MILLISECONDS.toDays(Calendar.getInstance().timeInMillis - lastCheck) > 0
//    }
}
