@file:Suppress("DEPRECATION")

package io.ffem.lite.ui

import android.app.Activity
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.database.FirebaseDatabase
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.camera.CameraFragment
import io.ffem.lite.common.*
import io.ffem.lite.data.AppDatabase
import io.ffem.lite.data.Result
import io.ffem.lite.databinding.ActivityBarcodeBinding
import io.ffem.lite.model.CalibrationValue
import io.ffem.lite.model.ErrorType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.AppPreferences
import io.ffem.lite.preference.AppPreferences.isCalibration
import io.ffem.lite.util.PreferencesUtil
import java.io.File
import java.io.File.separator
import java.util.*
import kotlin.math.round

const val DEBUG_MODE = "debugMode"
const val TEST_ID = "testId"

const val INSTRUCTION_PAGE = 0
const val CAMERA_PAGE = 1
const val CONFIRMATION_PAGE = 2
const val CALIBRATE_LIST_PAGE = 3
const val RESULT_PAGE = 4

/**
 * Activity to display info about the app.
 */
class BarcodeActivity : BaseActivity(),
    CalibrationItemFragment.OnCalibrationSelectedListener,
    InstructionFragment.OnStartTestListener,
    ImageConfirmFragment.OnConfirmImageListener {

    private lateinit var binding: ActivityBarcodeBinding
    private lateinit var broadcastManager: LocalBroadcastManager
    private var testInfo: TestInfo? = null
    lateinit var model: TestInfoViewModel
    lateinit var mediaPlayer: MediaPlayer

    private val colorCardCapturedBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mediaPlayer.start()
            deleteExcessData()
        }
    }

    private val resultBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            testInfo = intent.getParcelableExtra(TEST_INFO_KEY)

            if (testInfo != null) {
                model.setTest(testInfo)
                if (testInfo!!.error == ErrorType.BAD_LIGHTING ||
                    testInfo!!.error == ErrorType.IMAGE_TILTED
                ) {
                    binding.viewPager.currentItem = RESULT_PAGE
                } else {
                    binding.viewPager.currentItem = CONFIRMATION_PAGE
                }
            } else {
                binding.viewPager.currentItem = RESULT_PAGE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        binding = ActivityBarcodeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        broadcastManager = LocalBroadcastManager.getInstance(this)

        broadcastManager.registerReceiver(
            colorCardCapturedBroadcastReceiver,
            IntentFilter(CARD_CAPTURED_EVENT_BROADCAST)
        )

        broadcastManager.registerReceiver(
            resultBroadcastReceiver,
            IntentFilter(RESULT_EVENT_BROADCAST)
        )

        AppPreferences.generateImageFileName()

        if (BuildConfig.APPLICATION_ID == intent.action) {
            val uuid = intent.getStringExtra(TEST_ID)
            if (intent.getBooleanExtra(DEBUG_MODE, false)) {
                sendDummyResultForDebugging(uuid)
            }
            PreferencesUtil.setString(this, TEST_ID_KEY, uuid)
            PreferencesUtil.setBoolean(this, IS_CALIBRATION, false)
        } else {
            PreferencesUtil.removeKey(this, TEST_ID_KEY)
        }

        model = ViewModelProvider(this).get(
            TestInfoViewModel::class.java
        )

        binding.viewPager.isUserInputEnabled = false
        val testPagerAdapter = TestPagerAdapter(this)
        binding.viewPager.adapter = testPagerAdapter

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == CAMERA_PAGE || position == INSTRUCTION_PAGE) {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
                } else {
                    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                }
            }
        })

        mediaPlayer = MediaPlayer.create(this, R.raw.short_beep)
    }

    fun submitResult(@Suppress("UNUSED_PARAMETER") view: View) {
        if (testInfo != null) {
            val resultIntent = Intent()
            if (testInfo!!.getResult() >= 0) {
                sendResultToCloudDatabase(testInfo!!)
                resultIntent.putExtra(TEST_VALUE_KEY, testInfo!!.getResultString(this))
                resultIntent.putExtra(testInfo!!.name + "_Result", testInfo!!.getResultString(this))
                resultIntent.putExtra(testInfo!!.name + "_Risk", testInfo!!.getRiskEnglish(this))
                resultIntent.putExtra("meta_device", Build.BRAND + ", " + Build.MODEL)
            } else {
                resultIntent.putExtra(TEST_VALUE_KEY, "")
            }
            setResult(Activity.RESULT_OK, resultIntent)
        }
        finish()
    }

    private fun sendResultToCloudDatabase(testInfo: TestInfo) {
        if (!BuildConfig.INSTRUMENTED_TEST_RUNNING.get()) {
            val path = if (BuildConfig.DEBUG) {
                "result-debug"
            } else {
                "result"
            }
            val ref = FirebaseDatabase.getInstance().getReference(path).push()
            ref.setValue(
                Result(
                    testInfo.uuid!!,
                    testInfo.name!!,
                    testInfo.getRiskEnglish(this),
                    testInfo.getResultString(this),
                    testInfo.unit!!,
                    System.currentTimeMillis(),
                    App.getAppVersion(),
                    Build.MODEL
                )
            )
        }
    }

    private fun deleteExcessData() {
        val db = AppDatabase.getDatabase(baseContext)
        // Keep only last 25 results to save drive space
        try {
            for (i in 0..1) {
                if (db.resultDao().getCount() > 25) {
                    val result = db.resultDao().getOldestResult()

                    val path = getExternalFilesDir(DIRECTORY_PICTURES).toString() +
                            separator + "captures"
                    val directory = File("$path$separator${result.id}$separator")
                    if (directory.exists() && directory.isDirectory) {
                        directory.deleteRecursively()
                    }

                    db.resultDao().deleteResult(result.id)
                }
            }
        } finally {
            db.close()
        }
    }

    override fun onCalibrationSelected(calibrationValue: CalibrationValue) {
        model.test.get()!!.calibratedResultInfo.calibratedValue = calibrationValue
        pageNext()
    }

    /**
     * Create dummy results to send when in debug mode
     */
    private fun sendDummyResultForDebugging(uuid: String?) {
        if (uuid != null) {
            val testInfo = getTestInfo(uuid)
            if (testInfo != null) {
                val resultIntent = Intent()
                val random = Random()
                val maxValue = testInfo.values[testInfo.values.size / 2].value

                val result = (round(random.nextDouble() * maxValue * 100) / 100.0).toString()
                resultIntent.putExtra(TEST_VALUE_KEY, result)

                val pd = ProgressDialog(this)
                pd.setMessage("Sending dummy result...")
                pd.setCancelable(false)
                pd.show()

                setResult(Activity.RESULT_OK, resultIntent)
                Handler().postDelayed({
                    pd.dismiss()
                    finish()
                }, 3000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        broadcastManager.unregisterReceiver(colorCardCapturedBroadcastReceiver)
        broadcastManager.unregisterReceiver(resultBroadcastReceiver)
    }

    private fun pageBack() {
        if (binding.viewPager.currentItem in CAMERA_PAGE..CONFIRMATION_PAGE) {
            val testPagerAdapter = TestPagerAdapter(this)
            binding.viewPager.adapter = testPagerAdapter
        } else {
            binding.viewPager.currentItem = binding.viewPager.currentItem - 1
        }
    }

    private fun pageNext() {
        binding.viewPager.currentItem = binding.viewPager.currentItem + 1
    }

    override fun onBackPressed() {
        if (binding.viewPager.currentItem > INSTRUCTION_PAGE) {
            pageBack()
        } else {
            super.onBackPressed()
        }
    }

    class TestPagerAdapter(
        activity: AppCompatActivity
    ) : FragmentStateAdapter(activity) {

        var testInfo: TestInfo? = null

        override fun getItemCount(): Int {
            return if (isCalibration()) {
                5
            } else {
                4
            }
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                INSTRUCTION_PAGE -> {
                    InstructionFragment()
                }
                CAMERA_PAGE -> {
                    CameraFragment()
                }
                CONFIRMATION_PAGE -> {
                    ImageConfirmFragment()
                }
                CALIBRATE_LIST_PAGE -> {
                    if (isCalibration()) {
                        CalibrationItemFragment()
                    } else {
                        ResultFragment()
                    }
                }
                else -> {
                    if (isCalibration()) {
                        CalibrationResultFragment()
                    } else {
                        ResultFragment()
                    }
                }
            }
        }
    }

    override fun onStartTest() {
        binding.viewPager.currentItem = CAMERA_PAGE
    }

    override fun onConfirmImage(action: Int) {
        if (action == RESULT_OK) {
            if (isCalibration()) {
                if (model.test.get()?.error == ErrorType.NO_ERROR) {
                    binding.viewPager.currentItem = CALIBRATE_LIST_PAGE
                } else {
                    binding.viewPager.currentItem = RESULT_PAGE
                }
            } else {
                binding.viewPager.currentItem = CALIBRATE_LIST_PAGE
            }
        } else {
            val testPagerAdapter = TestPagerAdapter(this)
            binding.viewPager.adapter = testPagerAdapter
        }
    }
}
