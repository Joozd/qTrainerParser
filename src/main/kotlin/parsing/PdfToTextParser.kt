package parsing

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfPage
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor
import com.itextpdf.kernel.geom.Rectangle
import data.ParsedPage
import parsing.strategy.ColumnExtractorStrategy
import java.io.File
import java.text.Normalizer

class PdfToTextParser(val file: File) {
    constructor(fileName: String): this(File(fileName))

    private val inputStream get() = file.inputStream()

    fun parse(): String = inputStream.use{
        with (PdfDocument(PdfReader(inputStream))){
            if (numberOfPages == 0) ""
            else (1..numberOfPages).joinToString("\n") { getPage(it).readPage() }
        }
    }

    fun parseWithoutHeadersAndFooters(): String = inputStream.use {
        with(PdfDocument(PdfReader(inputStream))) {
            if (numberOfPages == 0) ""
            else (1..numberOfPages).joinToString("\n---\n") { p ->
                getPage(p).readPage().lines().filter{it.isNotBlank()}.drop(2).dropLast(3).joinToString("\n")
            }
        }
    }

    fun parsePages(): List<ParsedPage> = inputStream.use {
        with(PdfDocument(PdfReader(inputStream))) {
            var previousTopic: String? = null
            if (numberOfPages == 0) emptyList()
            else (1..numberOfPages).map {
                val rawPage = getPage(it).readPage()
                ParsedPage.of(rawPage, previousTopic).also { previousTopic = it.topic }.let{ pp ->
                    //If this is a page with Answers, we have to get rid of them anoying columns!
                    if (pp.isAnswers) pp.withContent(getPage(it).readColumns())
                    else pp
                }
            }
        }
    }

    private fun PdfPage.readPage(): String = PdfTextExtractor.getTextFromPage(this)

    private fun PdfPage.readColumns(): String {
        val wantedArea = Rectangle(pageSize.x, pageSize.y - FOOTER_SIZE, pageSize.width, pageSize.height - (HEADER_SIZE + FOOTER_SIZE))
        return PdfTextExtractor.getTextFromPage(this, ColumnExtractorStrategy(fixedColumns = 3, targetArea = wantedArea))
    }

    companion object{
        private const val FOOTER_SIZE = 30f
        private const val HEADER_SIZE = 30f
    }
}