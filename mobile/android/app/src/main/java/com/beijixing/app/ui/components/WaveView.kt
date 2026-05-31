package com.beijixing.app.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import androidx.core.content.ContextCompat
import com.beijixing.app.R

/**
 * 语音波形动画视图
 *
 * 用于语音助手页面展示语音识别时的声波动画效果
 *
 * @property waveCount 波条数量
 * @property waveColor 波条颜色
 * @property waveWidth 波条宽度
 * @property waveGap 波条间距
 */
class WaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.brand_primary)
    }

    // 波条数量
    private val waveCount = 5

    // 波条宽度
    private var waveWidth = 8f

    // 波条间距
    private var waveGap = 12f

    // 最大波条高度
    private var maxWaveHeight = 60f

    // 当前音量级别（0-100）
    private var volumeLevel = 0

    // 动画状态
    private var isAnimating = false

    // 动画旋转角度
    private var rotationAngle = 0f

    // 波条高度数组
    private var waveHeights = FloatArray(waveCount) { 10f }

    private var animation: RotateAnimation? = null

    init {
        // 读取自定义属性
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.WaveView,
            0, 0
        ).apply {
            try {
                waveWidth = getDimension(R.styleable.WaveView_waveWidth, 8f)
                waveGap = getDimension(R.styleable.WaveView_waveGap, 12f)
                maxWaveHeight = getDimension(R.styleable.WaveView_maxWaveHeight, 60f)
                paint.color = getColor(R.styleable.WaveView_waveColor,
                    ContextCompat.getColor(context, R.color.brand_primary))
            } finally {
                recycle()
            }
        }
    }

    /**
     * 设置音量级别
     *
     * @param level 音量级别（0-100）
     */
    fun setVolume(level: Int) {
        volumeLevel = level.coerceIn(0, 100)
        updateWaveHeights()
        invalidate()
    }

    /**
     * 开始动画
     */
    fun startAnimation() {
        isAnimating = true
        animation = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 2000
            interpolator = LinearInterpolator()
            repeatCount = Animation.INFINITE
            start()
        }
        invalidate()
    }

    /**
     * 停止动画
     */
    fun stopAnimation() {
        isAnimating = false
        animation?.cancel()
        animation = null
        invalidate()
    }

    /**
     * 更新波条高度
     */
    private fun updateWaveHeights() {
        val baseHeight = maxWaveHeight * 0.2f
        val variableHeight = maxWaveHeight * 0.8f * (volumeLevel / 100f)

        for (i in 0 until waveCount) {
            // 中间波条最高，向两边递减
            val positionFactor = 1 - Math.abs(i - waveCount / 2) / (waveCount / 2f + 0.5f)
            waveHeights[i] = baseHeight + variableHeight * positionFactor * (0.5f + Math.random().toFloat() * 0.5f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = (waveWidth * waveCount + waveGap * (waveCount - 1)).toInt() + paddingLeft + paddingRight
        val desiredHeight = maxWaveHeight.toInt() + paddingTop + paddingBottom

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> desiredWidth.coerceAtMost(widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val totalWidth = waveCount * waveWidth + (waveCount - 1) * waveGap
        val startX = centerX - totalWidth / 2f + waveWidth / 2f

        for (i in 0 until waveCount) {
            val x = startX + i * (waveWidth + waveGap)
            val waveHeight = if (isAnimating) waveHeights[i] else 10f

            // 绘制圆角矩形波条
            canvas.drawRoundRect(
                x - waveWidth / 2f,
                centerY - waveHeight / 2f,
                x + waveWidth / 2f,
                centerY + waveHeight / 2f,
                waveWidth / 2f,
                waveWidth / 2f,
                paint
            )
        }

        // 如果是动画状态，继续重绘
        if (isAnimating) {
            updateWaveHeights()
            postInvalidateOnAnimation()
        }
    }
}
