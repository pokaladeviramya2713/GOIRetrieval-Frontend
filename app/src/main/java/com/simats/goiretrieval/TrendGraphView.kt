package com.simats.goiretrieval

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class TrendGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var data = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
    private var rawData = intArrayOf(0, 0, 0, 0, 0, 0, 0)
    private var selectedIndex = -1
    
    private val path = Path()
    private val fillPath = Path()
    
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = Color.parseColor("#1A237E") // Deep Navy Blue
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val tooltipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        setShadowLayer(10f, 0f, 5f, Color.parseColor("#40000000"))
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A237E")
        textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    private var gradient: LinearGradient? = null
    private var animator: android.animation.ValueAnimator? = null

    fun setData(newData: IntArray) {
        this.rawData = newData
        val max = newData.maxOrNull()?.coerceAtLeast(1) ?: 1
        val targetData = newData.map { it.toFloat() / max.toFloat() }.toFloatArray()
        
        animator?.cancel()
        val oldData = data.copyOf()
        
        animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800
            interpolator = android.view.animation.DecelerateInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                for (i in data.indices) {
                    val startValue = if (i < oldData.size) oldData[i] else 0f
                    data[i] = startValue + (targetData[i] - startValue) * fraction
                }
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        when (event.action) {
            android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_MOVE -> {
                val stepX = width.toFloat() / (data.size - 1)
                val nearIndex = (event.x / stepX + 0.5f).toInt().coerceIn(0, data.size - 1)
                if (nearIndex != selectedIndex) {
                    selectedIndex = nearIndex
                    invalidate()
                }
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                // Keep selected or clear? I'll clear after 1 second for better UX
                postDelayed({
                    selectedIndex = -1
                    invalidate()
                }, 1500)
            }
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gradient = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            Color.parseColor("#401A237E"), // Navy semi-transparent
            Color.parseColor("#001A237E"), // Fully transparent
            Shader.TileMode.CLAMP
        )
        fillPaint.shader = gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()
        val stepX = w / (data.size - 1)
        
        path.reset()
        fillPath.reset()

        // Start from first point
        val startY = h - (data[0] * h * 0.6f) - (h * 0.2f)
        path.moveTo(0f, startY)
        fillPath.moveTo(0f, h)
        fillPath.lineTo(0f, startY)

        for (i in 1 until data.size) {
            val x = i * stepX
            val y = h - (data[i] * h * 0.6f) - (h * 0.2f)
            
            // Use cubic bezier for smooth waves
            val prevX = (i - 1) * stepX
            val prevY = h - (data[i - 1] * h * 0.6f) - (h * 0.2f)
            val conX1 = prevX + (stepX / 2f)
            val conX2 = prevX + (stepX / 2f)
            
            path.cubicTo(conX1, prevY, conX2, y, x, y)
            fillPath.cubicTo(conX1, prevY, conX2, y, x, y)
        }

        fillPath.lineTo(w, h)
        fillPath.close()

        canvas.drawPath(fillPath, fillPaint)
        canvas.drawPath(path, linePaint)
        
        // Draw Tooltip (Flag) if a point is selected
        if (selectedIndex != -1) {
            val x = selectedIndex * stepX
            val y = h - (data[selectedIndex] * h * 0.6f) - (h * 0.2f)
            
            // Get Day Name for the selected index
            val displaySdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            cal.add(java.util.Calendar.DAY_OF_YEAR, -(6 - selectedIndex))
            val dayName = if (6 - selectedIndex == 0) "Today" else displaySdf.format(cal.time)

            val countText = "${rawData[selectedIndex]}"
            val labelText = dayName
            
            val textWidth = Math.max(textPaint.measureText(countText), textPaint.measureText(labelText))
            val tooltipW = textWidth + 60f
            val tooltipH = 100f
            val tooltipBottom = y - 30f
            
            val rect = RectF(
                (x - tooltipW / 2).coerceAtLeast(10f).coerceAtMost(w - tooltipW - 10f),
                tooltipBottom - tooltipH,
                (x + tooltipW / 2).coerceAtLeast(tooltipW + 10f).coerceAtMost(w - 10f),
                tooltipBottom
            )
            
            // Draw Navy Flag bubble
            tooltipPaint.color = Color.parseColor("#1A237E")
            canvas.drawRoundRect(rect, 20f, 20f, tooltipPaint)
            
            // Draw small triangle
            val triPath = Path().apply {
                moveTo(x - 15f, tooltipBottom)
                lineTo(x + 15f, tooltipBottom)
                lineTo(x, tooltipBottom + 15f)
                close()
            }
            canvas.drawPath(triPath, tooltipPaint)
            
            // Draw Text (White)
            textPaint.color = Color.WHITE
            textPaint.textSize = 24f
            canvas.drawText(labelText, rect.centerX(), rect.top + 35f, textPaint)
            
            textPaint.textSize = 38f
            canvas.drawText(countText, rect.centerX(), rect.bottom - 20f, textPaint)
            
            // Highlight the point
            tooltipPaint.color = Color.WHITE
            canvas.drawCircle(x, y, 12f, linePaint)
            canvas.drawCircle(x, y, 6f, tooltipPaint)
        }
    }
}
