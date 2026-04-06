package com.simats.goiretrieval.utils

import android.content.Context
import android.net.Uri
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.StringBuilder

object DocumentTextExtractor {

    /**
     * Extracts raw text from any supported file URI.
     * Supports: PDF, TXT, CSV, JSON
     * Returns null if extraction fails.
     */
    suspend fun extractText(context: Context, uri: Uri, maxChars: Int = 12000): String? =
        withContext(Dispatchers.IO) {
            try {
                val mimeType = context.contentResolver.getType(uri) ?: ""
                val fileName = uri.path ?: ""
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                
                // More robust PDF detection
                val isPdf = mimeType.contains("pdf", ignoreCase = true) || 
                            fileName.endsWith(".pdf", ignoreCase = true) ||
                            uri.toString().contains(".pdf", ignoreCase = true)

                val text = when {
                    isPdf -> {
                        // Real PDF extraction using iTextG
                        inputStream.use { stream ->
                            try {
                                val reader = PdfReader(stream)
                                val sb = StringBuilder()
                                val numberOfPages = reader.numberOfPages
                                
                                android.util.Log.d("PDF_EXTRACT", "PDF opened, pages: $numberOfPages")
                                
                                for (i in 1..numberOfPages) {
                                    val pageText = PdfTextExtractor.getTextFromPage(reader, i)
                                    sb.append(pageText).append("\n")
                                    if (sb.length > maxChars) {
                                        android.util.Log.d("PDF_EXTRACT", "Reached maxChars limit at page $i")
                                        break
                                    }
                                }
                                reader.close()
                                val result = sb.toString()
                                android.util.Log.d("PDF_EXTRACT", "Extraction complete. Length: ${result.length}")
                                result
                            } catch (pdfEx: Exception) {
                                android.util.Log.e("PDF_EXTRACT", "iTextG Failed: ${pdfEx.message}")
                                // If it's a password protected or corrupted PDF, this hits
                                "Error reading PDF: ${pdfEx.message}"
                            }
                        }
                    }
                    else -> {
                        // Plain text, CSV, JSON — read directly
                        inputStream.bufferedReader().use { it.readText() }
                    }
                }

                if (text.isNullOrEmpty()) {
                    showToast(context, "⚠️ PDF contains no searchable text!")
                }

                // Trim to maxChars to avoid hitting Gemini token limits
                if (text.length > maxChars) {
                    text.take(maxChars) + "\n\n[...content truncated for AI processing...]"
                } else {
                    text
                }

            } catch (e: Exception) {
                android.util.Log.e("PDF_EXTRACT", "General Error: ${e.message}")
                showToast(context, "Error: ${e.message}")
                e.printStackTrace()
                null
            }
        }

    private fun showToast(context: Context, msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
