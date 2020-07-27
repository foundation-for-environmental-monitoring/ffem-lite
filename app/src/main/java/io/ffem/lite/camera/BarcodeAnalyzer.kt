package io.ffem.lite.camera

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import io.ffem.lite.BuildConfig
import io.ffem.lite.R
import io.ffem.lite.app.App
import io.ffem.lite.app.App.Companion.DEFAULT_TEST_UUID
import io.ffem.lite.app.App.Companion.TEST_INFO_KEY
import io.ffem.lite.app.App.Companion.getTestInfo
import io.ffem.lite.app.App.Companion.getTestName
import io.ffem.lite.model.ImageEdgeType
import io.ffem.lite.model.TestInfo
import io.ffem.lite.preference.getSampleTestImageNumberInt
import io.ffem.lite.preference.isDiagnosticMode
import io.ffem.lite.util.ColorUtil.fixBoundary
import io.ffem.lite.util.ColorUtil.isBarcodeValid
import io.ffem.lite.util.ColorUtil.isTilted
import io.ffem.lite.util.getBitmapPixels
import io.ffem.lite.util.isNotBright
import java.util.*
import kotlin.math.max
import kotlin.math.min


const val MAX_ANGLE = 14

class BarcodeAnalyzer(private val context: Context) : ImageAnalysis.Analyzer {

    companion object {
        private var capturePhoto: Boolean = false
        private var processing = false
        private var done: Boolean = false
        var autoFocusCounter = 0
    }

    private lateinit var bitmap: Bitmap
    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val detector: FirebaseVisionBarcodeDetector by lazy {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                FirebaseVisionBarcode.FORMAT_CODE_128
            )
            .build()
        FirebaseVision.getInstance().getVisionBarcodeDetector(options)
    }

    private var taskLeftBarcode: Task<out Any>? = null
    private lateinit var mediaImage: FirebaseVisionImage

    override fun analyze(image: ImageProxy) {
        if (done || processing) {
            return
        }
        processing = true

        @ExperimentalGetImage
        mediaImage = FirebaseVisionImage.fromMediaImage(
            image.image!!, FirebaseVisionImageMetadata.ROTATION_180
        )

        localBroadcastManager = LocalBroadcastManager.getInstance(context)

        if (BuildConfig.DEBUG && (isDiagnosticMode() || BuildConfig.INSTRUMENTED_TEST_RUNNING.get())) {
            val imageNumber = getSampleTestImageNumberInt()
            if (imageNumber > -1) {
                try {
                    val drawable = ContextCompat.getDrawable(
                        context, context.resources.getIdentifier(
                            "test_${java.lang.String.format(Locale.ROOT, "%03d", imageNumber)}",
                            "drawable", context.packageName
                        )
                    )
                    bitmap = (drawable as BitmapDrawable).bitmap
                } catch (ex: Exception) {
                    sendMessage(context.getString(R.string.sample_image_not_found))
                    endProcessing(image, true)
                    return
                }
            } else {
                bitmap = mediaImage.bitmap
            }
        } else {
            bitmap = mediaImage.bitmap
        }

        bitmap = Bitmap.createBitmap(
            bitmap, bitmap.width / 2, 0,
            bitmap.width / 2,
            bitmap.height
        )

        if (capturePhoto) {
            done = true
            val testInfo = getTestInfo(DEFAULT_TEST_UUID)!!
            testInfo.fileName = UUID.randomUUID().toString()
            savePhoto(bitmap, testInfo)
            endProcessing(image, true)
            return
        }

        var badLighting = false

        var rect = Rect(100, 0, bitmap.width - 100, 10)
        var pixels = getBitmapPixels(bitmap, rect)
        if (isNotBright(pixels)) {
            endProcessing(image, true)
            return
        }

        rect = Rect(100, bitmap.height - 10, bitmap.width - 100, bitmap.height)
        pixels = getBitmapPixels(bitmap, rect)
        if (isNotBright(pixels)) {
            endProcessing(image, true)
            return
        }

        val leftBarcodeBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height / 2
        )

        taskLeftBarcode =
            detector.detectInImage(FirebaseVisionImage.fromBitmap(leftBarcodeBitmap))
                .addOnFailureListener(
                    fun(_: Exception) {
                        sendMessage(context.getString(R.string.color_card_not_found))
                        endProcessing(image, true)
                        return
                    }
                )
                .addOnSuccessListener(
                    fun(result: List<FirebaseVisionBarcode>) {
                        if (result.isEmpty()) {
                            sendMessage(context.getString(R.string.color_card_not_found))
                            endProcessing(image, true)
                            return
                        }
                        for (leftBarcode in result) {
                            if (!leftBarcode.rawValue.isNullOrEmpty()) {
                                var testName = getTestName(result[0].displayValue!!)
                                if (testName.isEmpty()) {
                                    sendMessage(context.getString(R.string.invalid_barcode))
                                    endProcessing(image, true)
                                    return
                                }

                                if (autoFocusCounter < 10) {
                                    autoFocusCounter++
                                    endProcessing(image, false)
                                    return
                                }

                                try {
                                    val leftBoundingBox =
                                        fixBoundary(
                                            leftBarcode,
                                            leftBarcodeBitmap,
                                            ImageEdgeType.WhiteTop
                                        )

//                                    Timber.e(
//                                        "Bar Left: %s %s %s %s", leftBoundingBox.left,
//                                        leftBoundingBox.top,
//                                        leftBoundingBox.right,
//                                        leftBoundingBox.bottom
//                                    )

                                    if (leftBoundingBox.top in 11..80) {

                                        if (!isBarcodeValid(
                                                leftBarcodeBitmap,
                                                leftBoundingBox,
                                                ImageEdgeType.WhiteTop
                                            )
                                        ) {
                                            badLighting = true
                                            leftBarcodeBitmap.recycle()
                                            endProcessing(image, false)
                                            return
                                        }

                                        leftBarcodeBitmap.recycle()

                                        val rightBarcodeBitmap = Bitmap.createBitmap(
                                            bitmap, 0, bitmap.height / 2,
                                            bitmap.width, bitmap.height / 2
                                        )

                                        detector.detectInImage(
                                            FirebaseVisionImage.fromBitmap(rightBarcodeBitmap)
                                        )
                                            .addOnFailureListener(fun(_: Exception) {
//                                                sendMessage(context.getString(R.string.color_card_not_found) + "...")
                                                endProcessing(image, true)
                                                return
                                            })
                                            .addOnSuccessListener(
                                                fun(result: List<FirebaseVisionBarcode>) {
                                                    if (result.isNullOrEmpty()) {
//                                                        sendMessage(context.getString(R.string.color_card_not_found) + "....")
                                                        endProcessing(image, true)
                                                        return
                                                    }

                                                    for (rightBarcode in result) {

                                                        val rightBoundingBox =
                                                            fixBoundary(
                                                                rightBarcode,
                                                                rightBarcodeBitmap,
                                                                ImageEdgeType.WhiteDown
                                                            )

                                                        if (rightBarcodeBitmap.height - rightBoundingBox.bottom !in 11..80) {
                                                            rightBarcodeBitmap.recycle()
                                                            endProcessing(image, false)
                                                            return
                                                        }

                                                        if (isTilted(
                                                                leftBoundingBox, rightBoundingBox
                                                            )
                                                        ) {
                                                            sendMessage(context.getString(R.string.correct_camera_tilt))
                                                            endProcessing(image, false)
                                                            return
                                                        }

                                                        testName =
                                                            getTestName(result[0].displayValue!!)
                                                        if (testName.isEmpty()) {
                                                            sendMessage(context.getString(R.string.invalid_barcode))
                                                            endProcessing(image, false)
                                                            return
                                                        }

                                                        if (badLighting || !isBarcodeValid(
                                                                rightBarcodeBitmap,
                                                                rightBoundingBox,
                                                                ImageEdgeType.WhiteDown
                                                            )
                                                        ) {
                                                            sendMessage(context.getString(R.string.try_moving_well_lit))
                                                            rightBarcodeBitmap.recycle()
                                                            endProcessing(image, false)
                                                            return
                                                        }

                                                        rightBarcodeBitmap.recycle()

                                                        analyzeBarcode(
                                                            image,
                                                            bitmap,
                                                            rightBarcode,
                                                            rightBoundingBox,
                                                            leftBoundingBox
                                                        )
                                                    }
                                                }
                                            )
                                    } else {
                                        sendMessage(context.getString(R.string.color_card_not_found))
                                        endProcessing(image, true)
                                    }
                                } catch (ignored: Exception) {
                                    endProcessing(image, true)
                                }
                            } else {
                                endProcessing(image, true)
                            }
                        }
                    }
                )
    }

    private fun endProcessing(image: ImageProxy, reset: Boolean) {
        if (::bitmap.isInitialized) {
            bitmap.recycle()
        }
        processing = false
        if (reset) {
            autoFocusCounter = 0
        }
        image.close()
    }

    private fun analyzeBarcode(
        image: ImageProxy,
        bitmap: Bitmap, rightBarcode: FirebaseVisionBarcode,
        rightBoundingBox: Rect, leftBoundingBox: Rect
    ) {
        if (!rightBarcode.rawValue.isNullOrEmpty()) {
            val testInfo = getTestInfo(rightBarcode.displayValue!!)
            if (testInfo == null) {
                sendMessage(context.getString(R.string.invalid_barcode))
                endProcessing(image, false)
                return
            }

            done = true

            val cropLeft = max(leftBoundingBox.left - 20, 0)
            val cropWidth = min(
                leftBoundingBox.right - cropLeft + 40,
                bitmap.width - cropLeft
            )
            val cropTop = max(leftBoundingBox.top - 40, 0)
            val cropHeight = min(
                rightBoundingBox.bottom - leftBoundingBox.top + (bitmap.height / 2) + 80,
                bitmap.height - cropTop
            )

            val finalBitmap = Bitmap.createBitmap(
                bitmap, cropLeft, cropTop, cropWidth, cropHeight
            )

            testInfo.fileName = UUID.randomUUID().toString()
            savePhoto(finalBitmap, testInfo)

            finalBitmap.recycle()

            endProcessing(image, true)

        } else {
            endProcessing(image, true)
        }
    }

    private fun savePhoto(bitmap: Bitmap, testInfo: TestInfo) {

        val bitmapRotated = Utilities.rotateImage(bitmap, 270)

        Utilities.savePicture(
            context.applicationContext,
            testInfo.fileName,
            testInfo.name!!,
            Utilities.bitmapToBytes(bitmapRotated),
            isExtract = false,
            isGrayscale = false
        )

        bitmapRotated.recycle()

        val intent = Intent(App.CAPTURED_EVENT)
        intent.putExtra(TEST_INFO_KEY, testInfo)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    private fun sendMessage(s: String) {
        val intent = Intent(App.ERROR_EVENT)
        intent.putExtra(App.ERROR_MESSAGE, s)
        localBroadcastManager.sendBroadcast(
            intent
        )
    }

    fun takePhoto() {
        capturePhoto = true
    }

    fun reset() {
        done = false
        processing = false
        capturePhoto = false
        autoFocusCounter = 0
    }
}
