package com.example.privytaskai.data.pdf

import android.util.Log
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

/**
 * PDF reader for extracting text from SEBI circulars
 *
 * Features:
 * - Page-by-page text extraction
 * - Metadata extraction (title, author, creation date)
 * - Error handling for corrupted PDFs
 */
class PDFReader {

    companion object {
        private const val TAG = "PDFReader"
    }

    /**
     * PDF document metadata
     */
    data class PDFMetadata(
        val title: String?,
        val author: String?,
        val subject: String?,
        val creationDate: String?,
        val numberOfPages: Int
    )

    /**
     * PDF extraction result
     */
    data class PDFDocument(
        val text: String,
        val metadata: PDFMetadata,
        val pageTexts: List<String>
    )

    /**
     * Extract text from PDF input stream
     *
     * @param inputStream PDF file input stream
     * @return PDFDocument with text and metadata
     */
    suspend fun extractText(inputStream: InputStream): Result<PDFDocument> =
        withContext(Dispatchers.IO) {
            var reader: PdfReader? = null

            try {
                Log.d(TAG, "Starting PDF text extraction...")

                reader = PdfReader(inputStream)
                val numberOfPages = reader.numberOfPages

                Log.d(TAG, "PDF has $numberOfPages pages")

                // Extract metadata
                val metadata = extractMetadata(reader)

                // Extract text from all pages
                val pageTexts = mutableListOf<String>()
                val fullText = StringBuilder()

                for (page in 1..numberOfPages) {
                    try {
                        val pageText = PdfTextExtractor.getTextFromPage(reader, page)
                        pageTexts.add(pageText)
                        fullText.append(pageText).append("\n\n")

                        Log.d(TAG, "Extracted page $page (${pageText.length} chars)")

                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to extract page $page", e)
                        pageTexts.add("")
                    }
                }

                val document = PDFDocument(
                    text = fullText.toString(),
                    metadata = metadata,
                    pageTexts = pageTexts
                )

                Log.d(TAG, "PDF extraction complete: ${fullText.length} total chars")
                Result.success(document)

            } catch (e: Exception) {
                Log.e(TAG, "PDF extraction failed", e)
                Result.failure(e)

            } finally {
                reader?.close()
            }
        }

    /**
     * Extract metadata from PDF
     */
    private fun extractMetadata(reader: PdfReader): PDFMetadata {
        val info = reader.info

        return PDFMetadata(
            title = info["Title"],
            author = info["Author"],
            subject = info["Subject"],
            creationDate = info["CreationDate"],
            numberOfPages = reader.numberOfPages
        )
    }
}
