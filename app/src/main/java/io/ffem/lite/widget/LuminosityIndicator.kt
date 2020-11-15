package io.ffem.lite.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import io.ffem.lite.R
import kotlin.math.max
import kotlin.math.min

class LuminosityIndicator : View {
    var luminosity: Int = 0
    private var barPaint: Paint = Paint()
    private var redPaint: Paint = Paint()
    private var yellowPaint: Paint = Paint()
    private var greenPaint: Paint = Paint()
    private val path = Path()

    private var redZone1: Int = 0
    private var redZone2: Int = 0
    private var greenZoneStart: Int = 0
    private var greenZoneEnd: Int = 0

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        barPaint.strokeWidth = 10f
        barPaint.isAntiAlias = true

        redPaint.color = Color.RED
        yellowPaint.color = Color.YELLOW
        greenPaint.color = ResourcesCompat.getColor(resources, R.color.bright_green, null)
    }

    @SuppressLint("DrawAllocation")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (barPaint.shader == null) {
            val shader: Shader = LinearGradient(
                0f, 0f, measuredWidth.toFloat(), 0f,
                ResourcesCompat.getColor(resources, R.color.black, null),
                ResourcesCompat.getColor(resources, R.color.white, null), Shader.TileMode.MIRROR
            )
            barPaint.shader = shader
        }

        val interval = measuredWidth / 6
        redZone1 = interval
        greenZoneStart = interval * 2
        greenZoneEnd = interval * 4
        redZone2 = interval * 5

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        path.reset()
        canvas.drawRect(20f, 36f, measuredWidth.toFloat() - 20, 56f, barPaint)

        val pos = max(
            min((luminosity * measuredWidth.toFloat()) / 4000, measuredWidth.toFloat() - 20),
            20f
        )

        path.moveTo(pos - 20, 0f)
        path.lineTo(pos + 20, 0f)
        path.lineTo(pos, 30f)
        path.close()


        if (pos <= redZone1 || pos >= redZone2) {
            canvas.drawPath(path, redPaint)
        } else if ((pos > redZone1 && pos < greenZoneStart) || (pos > greenZoneEnd && pos < redZone2)) {
            canvas.drawPath(path, yellowPaint)
        } else {
            canvas.drawPath(path, greenPaint)
        }
    }
}