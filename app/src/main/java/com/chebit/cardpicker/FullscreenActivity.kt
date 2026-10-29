package com.chebit.cardpicker

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import java.util.concurrent.Executors
import kotlin.math.abs

class FullscreenActivity : Activity() {

    private lateinit var imageView: ImageView
    private lateinit var counter: TextView
    private lateinit var hint: TextView
    private lateinit var closeButton: View
    private lateinit var gestures: GestureDetector

    private var items: List<ImageRef> = emptyList()
    private var index = 0
    private var flingHandled = false
    private val pool = Executors.newFixedThreadPool(2)

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        imageView = findViewById(R.id.fullImage)
        counter = findViewById(R.id.txtCounter)
        hint = findViewById(R.id.txtHint)
        closeButton = findViewById(R.id.btnClose)

        items = SelectedImages.items
        if (items.isEmpty()) {
            finish()
            return
        }

        index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, items.size - 1)
        closeButton.setOnClickListener { finish() }

        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                if (abs(dx) > 80 && abs(velocityX) > 120) {
                    flingHandled = true
                    if (dx < 0) show(index + 1) else show(index - 1)
                    return true
                }
                return false
            }
        })

        imageView.setOnClickListener { toggleControls() }
        imageView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) flingHandled = false
            gestures.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP && !flingHandled) {
                view.performClick()
            }
            true
        }

        show(index)
    }

    private fun toggleControls() {
        val visibility = if (hint.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        hint.visibility = visibility
        counter.visibility = visibility
        closeButton.visibility = visibility
    }

    private fun show(newIndex: Int) {
        if (items.isEmpty()) return
        index = when {
            newIndex < 0 -> items.size - 1
            newIndex >= items.size -> 0
            else -> newIndex
        }

        counter.text = getString(R.string.counter, index + 1, items.size)

        val item = items[index]
        val key = item.uri.toString()
        imageView.setImageDrawable(null)
        imageView.tag = key

        pool.execute {
            val bitmap = ImageLoader.load(
                this,
                item.uri,
                1600,
                1600,
                cacheKey = "full:$key"
            )
            runOnUiThread {
                if (imageView.tag == key && bitmap != null) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    override fun onDestroy() {
        pool.shutdownNow()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_INDEX = "index"
    }
}
