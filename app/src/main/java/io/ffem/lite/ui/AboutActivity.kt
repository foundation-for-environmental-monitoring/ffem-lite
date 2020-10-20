package io.ffem.lite.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.toast
import kotlinx.android.synthetic.main.activity_about.*

/**
 * Activity to display info about the app.
 */
class AboutActivity : BaseActivity() {

    private var clickCount = 0

    private var dialog: NoticesDialogFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        textVersion.text = App.getAppVersion()

        setTitle(R.string.about)
    }

    /**
     * Displays legal information.
     */
    fun onSoftwareNoticesClick(@Suppress("UNUSED_PARAMETER") view: View) {
        dialog = NoticesDialogFragment.newInstance()
        dialog!!.show(supportFragmentManager, "NoticesDialog")
    }

    /**
     * Disables diagnostic mode.
     */
    fun disableDiagnosticsMode(@Suppress("UNUSED_PARAMETER") view: View) {
        toast(getString(R.string.diagnosticModeDisabled))

        AppPreferences.disableDiagnosticMode()

        switchLayoutForDiagnosticOrUserMode()

        changeActionBarStyleBasedOnCurrentMode()

        finish()
    }

    /**
     * Turn on diagnostic mode if user clicks on version section CHANGE_MODE_MIN_CLICKS times.
     */
    fun switchToDiagnosticMode(@Suppress("UNUSED_PARAMETER") view: View) {
        if (!isDiagnosticMode()) {
            clickCount++

            if (clickCount >= CHANGE_MODE_MIN_CLICKS) {
                clickCount = 0
                toast(getString(R.string.diagnosticModeEnabled))

                AppPreferences.enableDiagnosticMode()

                changeActionBarStyleBasedOnCurrentMode()

                switchLayoutForDiagnosticOrUserMode()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        switchLayoutForDiagnosticOrUserMode()
    }

    /**
     * Show the diagnostic mode layout.
     */
    private fun switchLayoutForDiagnosticOrUserMode() {
        if (isDiagnosticMode()) {
            layoutDiagnostics.visibility = View.VISIBLE
        } else {
            if (layoutDiagnostics.visibility == View.VISIBLE) {
                layoutDiagnostics.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val CHANGE_MODE_MIN_CLICKS = 10
    }
}
