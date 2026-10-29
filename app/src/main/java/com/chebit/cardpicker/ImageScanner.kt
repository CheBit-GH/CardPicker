package com.chebit.cardpicker

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

object ImageScanner {
    private const val MAX_DEPTH = 8

    private val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE
    )

    fun scan(context: Context, treeUri: Uri): List<ImageRef> {
        val result = ArrayList<ImageRef>()
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        walk(context, treeUri, rootId, result, 0)
        return result.sortedBy { it.name.lowercase() }
    }

    private fun walk(
        context: Context,
        treeUri: Uri,
        parentDocumentId: String,
        out: MutableList<ImageRef>,
        depth: Int
    ) {
        if (depth > MAX_DEPTH) return

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val cursor = try {
            context.contentResolver.query(childrenUri, projection, null, null, null)
        } catch (_: Exception) {
            null
        } ?: return

        cursor.use { c ->
            while (c.moveToNext()) {
                val id = c.getString(0) ?: continue
                val name = c.getString(1) ?: ""
                val mime = c.getString(2) ?: ""

                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    walk(context, treeUri, id, out, depth + 1)
                } else if (mime.startsWith("image/")) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    out.add(ImageRef(name, uri))
                }
            }
        }
    }
}
