package com.hexoskin.astroskinhexoskindemo

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CanvasView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var thoracicVal = 26000
    private var abdominalVal = 15000
    private val circleX = 600f
    private val circleY = 1000f
    private val thoracicPaint = Paint()
    private val abdominalPaint = Paint()

    fun update(thoracic : Int, abdominal : Int) {
        thoracicVal = thoracic
        abdominalVal = abdominal

        // use these to make the draw method call itself again
        bringToFront()
        invalidate()
        requestLayout()
    }

    private fun convert(input: Int, inMin: Int, inMax: Int, outMin: Int, outMax: Int): Float {
        return ((outMax - outMin) * (input.toFloat() - inMin) / (inMax - inMin) + outMin)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // the range values should be fit to each user
        val thoracicMin = 24000
        val thoracicMax = 26000
        val abdominalMin = 14600
        val abdominalMax = 15000
        val convertMin = 240
        val convertMax = 0

        // convert values to appropriate ranges
        val thoracicConvert = convert(thoracicVal, thoracicMin, thoracicMax, convertMin, convertMax)
        val abdominalConvert= convert(abdominalVal, abdominalMin, abdominalMax, convertMin, convertMax)

        /*
        // circle visualization
        thoracicPaint.setColor(Color.HSVToColor(50, floatArrayOf(355f, 60f, 76f)))
        abdominalPaint.setColor(Color.HSVToColor(50, floatArrayOf(222f, 70f, 52f)))

        canvas?.drawCircle(circleX, circleY, thoracicRadius, thoracicPaint)
        canvas?.drawCircle(circleX, circleY, abdominalRadius, abdominalPaint)
        */

        // rectangle visualization
        val screenWidth = width
        val screenHeight = height

        thoracicPaint.setColor(Color.HSVToColor(50, floatArrayOf(thoracicConvert, 1f, 1f)))
        abdominalPaint.setColor(Color.HSVToColor(50, floatArrayOf(abdominalConvert, 1f, 1f)))

        canvas?.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), abdominalPaint)
    }
}

