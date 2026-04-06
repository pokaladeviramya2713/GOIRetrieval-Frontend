package com.simats.goiretrieval.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import okhttp3.ResponseBody
import java.io.OutputStream

object AuditReportUtils {

    fun saveReportToDownloads(context: Context, responseBody: ResponseBody, fileName: String) {
        try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                // Legacy support for older Android versions
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = java.io.File(directory, fileName)
                Uri.fromFile(file)
            }

            uri?.let { targetUri ->
                val outputStream: OutputStream? = contentResolver.openOutputStream(targetUri)
                outputStream?.use { os ->
                    responseBody.byteStream().use { inputStream ->
                        inputStream.copyTo(os)
                    }
                }
                Toast.makeText(context, "Report saved to Downloads!", Toast.LENGTH_LONG).show()
                
                // Prompt to open
                try {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        setDataAndType(targetUri, "application/pdf")
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(android.content.Intent.createChooser(intent, "Open Audit Report"))
                } catch (e: Exception) {
                    // No PDF viewer found
                }
            } ?: throw Exception("Failed to create file URI")

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Download Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
