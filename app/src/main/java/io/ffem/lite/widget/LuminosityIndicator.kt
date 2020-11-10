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
    var luminosity: Float = 0f
    private var barPaint: Paint = Paint()
    private var redPaint: Paint = Paint()
    private var greenPaint: Paint = Paint()
    private val path = Path()

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
        if (pos < measuredWidth / 4 || pos > measuredWidth - (measuredWidth / 4)) {
            canvas.drawPath(path, redPaint)
        } else {
            canvas.drawPath(path, greenPaint)
        }
    }
}