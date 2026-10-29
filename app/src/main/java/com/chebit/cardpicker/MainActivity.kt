package com.chebit.cardpicker

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var btnDraw: Button
    private lateinit var txtFolder: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtEmpty: TextView
    private lateinit var inputCount: EditText
    private lateinit var gridContainer: LinearLayout

    private var treeUri: Uri? = null
    private var allImages: List<ImageRef> = emptyList()
    private var drawn: List<ImageRef> = emptyList()

    private val pool = Executors.newFixedThreadPool(4)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("card_picker", MODE_PRIVATE)

        val btnChoose = findViewById<Button>(R.id.btnChoose)
        btnDraw = findViewById(R.id.btnDraw)
        txtFolder = findViewById(R.id.txtFolder)
        txtStatus = findViewById(R.id.txtStatus)
        txtEmpty = findViewById(R.id.txtEmpty)
        inputCount = findViewById(R.id.inputCount)
        gridContainer = findViewById(R.id.gridContainer)

        inputCount.setText(prefs.getInt("count", 5).toString())
        inputCount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) saveCount()
        }

        btnChoose.setOnClickListener { chooseFolder() }
        btnDraw.setOnClickListener { drawCards() }

        val saved = prefs.getString("tree_uri", null)
        if (saved != null) {
            treeUri = Uri.parse(saved)
            scanFolder()
        }
    }

    private fun saveCount() {
        val value = inputCount.text.toString().toIntOrNull() ?: 5
        prefs.edit().putInt("count", value.coerceIn(1, 99)).apply()
    }

    private fun chooseFolder() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            )
        }
        @Suppress("DEPRECATION")
        startActivityForResult(intent, REQUEST_FOLDER)
    }

    @Deprecated("Kept simple on purpose: no AndroidX dependency")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_FOLDER || resultCode != RESULT_OK) return

        val uri = data?.data ?: return
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some providers cannot persist permission; the scan still works
            // for this session.
        }

        treeUri = uri
        prefs.edit().putString("tree_uri", uri.toString()).apply()
        clearDrawn()
        scanFolder()
    }

    private fun scanFolder() {
        val uri = treeUri ?: return
        btnDraw.isEnabled = false
        txtStatus.text = getString(R.string.scanning)
        txtEmpty.visibility = View.GONE

        Thread {
            val list = try {
                ImageScanner.scan(this, uri)
            } catch (_: SecurityException) {
                null
            }

            runOnUiThread {
                if (list == null) {
                    txtStatus.text = getString(R.string.permission_lost)
                    btnDraw.isEnabled = false
                    return@runOnUiThread
                }

                allImages = list
                val folderName = uri.lastPathSegment?.substringAfterLast(':') ?: ""
                txtFolder.text = if (list.isEmpty()) {
                    getString(R.string.no_images)
                } else {
                    resources.getQuantityString(
                        R.plurals.image_count,
                        list.size,
                        folderName.ifEmpty { "Folder" },
                        list.size
                    )
                }
                btnDraw.isEnabled = list.isNotEmpty()
                txtStatus.text = ""
                if (list.isEmpty()) txtEmpty.visibility = View.VISIBLE
            }
        }.start()
    }

    private fun drawCards() {
        if (allImages.isEmpty()) return
        saveCount()

        val requested = inputCount.text.toString().toIntOrNull() ?: 0
        if (requested < 1 || requested > allImages.size) {
            txtStatus.text = getString(R.string.bad_count, allImages.size)
            return
        }

        // Fisher-Yates: every card is equally likely, no duplicates.
        val shuffled = allImages.toMutableList()
        for (i in shuffled.size - 1 downTo 1) {
            val j = (Math.random() * (i + 1)).toInt()
            val swap = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = swap
        }

        drawn = shuffled.take(requested)
        SelectedImages.items = drawn
        renderHand()
        txtStatus.text = resources.getQuantityString(
            R.plurals.drew_count,
            drawn.size,
            drawn.size,
            allImages.size
        )
        txtEmpty.visibility = View.GONE
    }

    private fun clearDrawn() {
        drawn = emptyList()
        SelectedImages.items = emptyList()
        gridContainer.removeAllViews()
        txtEmpty.visibility = View.VISIBLE
    }

    private fun renderHand() {
        gridContainer.removeAllViews()
        if (drawn.isEmpty()) return

        val density = resources.displayMetrics.density
        val gap = (8 * density).toInt()
        val cellHeight = (140 * density).toInt()
        val columns = 3

        var start = 0
        while (start < drawn.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                if (start > 0) setPadding(0, gap, 0, 0)
            }

            for (offset in 0 until columns) {
                val position = start + offset
                if (position >= drawn.size) break

                val item = drawn[position]
                val key = item.uri.toString()
                val cell = layoutInflater.inflate(R.layout.item_image, row, false)
                val params = LinearLayout.LayoutParams(0, cellHeight, 1f)
                if (offset > 0) params.marginStart = gap
                cell.layoutParams = params
                cell.setOnClickListener { openFullscreen(position) }

                val img = cell.findViewById<ImageView>(R.id.img)
                img.tag = key

                pool.execute {
                    val bitmap = ImageLoader.load(
                        this,
                        item.uri,
                        320,
                        320,
                        cacheKey = "thumb:$key"
                    )
                    runOnUiThread {
                        if (img.tag == key && bitmap != null) {
                            img.setImageBitmap(bitmap)
                        }
                    }
                }

                row.addView(cell)
            }

            gridContainer.addView(row)
            start += columns
        }
    }

    private fun openFullscreen(position: Int) {
        if (drawn.isEmpty()) return
        startActivity(
            Intent(this, FullscreenActivity::class.java)
                .putExtra(FullscreenActivity.EXTRA_INDEX, position)
        )
    }

    override fun onDestroy() {
        pool.shutdownNow()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_FOLDER = 1001
    }
}
