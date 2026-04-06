package com.simats.goiretrieval.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.widget.Toast
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.pdf.draw.LineSeparator
import com.simats.goiretrieval.ActivityLogger
import com.simats.goiretrieval.api.AuditDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object AuditPdfGenerator {

    fun generateAuditReport(context: Context, doc: AuditDocument) {
        val fileName = "${doc.title?.replace(" ", "_") ?: "Audit_Report"}_${System.currentTimeMillis()}.pdf"
        
        try {
            val contentResolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: throw Exception("Failed to create MediaStore entry")

            contentResolver.openOutputStream(uri)?.use { outputStream ->
                val document = Document()
                PdfWriter.getInstance(document, outputStream)
                document.open()

                // Header Section
                val titleFont = Font(Font.FontFamily.HELVETICA, 18f, Font.BOLD, BaseColor(26, 35, 126)) // Navy Home Color
                val subTitleFont = Font(Font.FontFamily.HELVETICA, 12f, Font.ITALIC, BaseColor.GRAY)
                val headingFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD, BaseColor.BLACK)
                val bodyFont = Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL, BaseColor.BLACK)
                val scoreFont = Font(Font.FontFamily.HELVETICA, 16f, Font.BOLD, BaseColor(76, 175, 80)) // Green

                document.add(Paragraph("GOI RETRIEVAL - EXECUTIVE AI AUDIT", titleFont))
                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                document.add(Paragraph("Generated on: ${sdf.format(Date())}", subTitleFont))
                document.add(Paragraph("Report Category: ${doc.category ?: "Regulatory Analysis"}", subTitleFont))
                document.add(Chunk(LineSeparator()))
                document.add(Paragraph(" "))

                document.add(Paragraph("REPORT TITLE: ${doc.title}", headingFont))
                document.add(Paragraph("COMPLIANCE SCORE: ${doc.compliance_score}%", scoreFont))
                document.add(Paragraph(" "))

                val cleanSummary = (doc.summary ?: "No summary available.").replace(Regex("<[^>]*>"), "")
                document.add(Paragraph("AI COMPLIANCE SUMMARY", headingFont))
                document.add(Paragraph(cleanSummary, bodyFont))
                document.add(Paragraph(" "))

                val cleanAnalysis = (doc.ai_analysis ?: "No detailed analysis provided.").replace(Regex("<[^>]*>"), "")
                document.add(Paragraph("DETAILED ANALYSIS", headingFont))
                document.add(Paragraph(cleanAnalysis, bodyFont))
                document.add(Paragraph(" "))

                val cleanContent = (doc.original_content ?: "Original segment text was not available.").replace(Regex("<[^>]*>"), "")
                document.add(Paragraph("ORIGINAL CONTENT SNIPPET", headingFont))
                document.add(Paragraph(cleanContent, bodyFont))
                document.add(Paragraph(" "))

                document.add(Chunk(LineSeparator()))
                val footerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL, BaseColor.LIGHT_GRAY)
                val footerText = Paragraph("This report was automatically generated via GOI Retrieval AI.", footerFont)
                footerText.alignment = Element.ALIGN_CENTER
                document.add(footerText)

                document.close()
            }

            // Show Toast
            Toast.makeText(context, "Report saved to Downloads!", Toast.LENGTH_LONG).show()
            ActivityLogger.log(context, "Download", "Generated Audit PDF: ${doc.title}", doc.id)

            // OPTIONAL: Immediately prompt to open the file
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(intent, "Open Audit Report"))
            } catch (e: Exception) {
                // If no PDF viewer found, user can still find it in Downloads
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}