package parsing.strategy

import com.itextpdf.kernel.geom.Matrix
import com.itextpdf.kernel.geom.Rectangle
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.filter.TextRegionEventFilter
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy.ITextChunkLocationStrategy
import com.itextpdf.kernel.pdf.canvas.parser.listener.TextChunk


/**
 * Extract text in columns.
 * @param clStrat: Text extractor parsing.strategy
 * @param fixedColumns: Fixed amount of columns. Columns can be empty, evenly spaced. If null, will try to find them another way
 * @param targetArea: Only read text within this area
 * @param minWidth: Minimum column width
 *
 * @param maxColumns: Max number of columns to be found
 * @param columnStartMarker: A word that is always on the left side of all columns
 * @param columnEndMarker: a word that is always on the right side of all columns
 */
class ColumnExtractorStrategy(private val clStrat: ITextChunkLocationStrategy = ITextChunkLocationStrategy { renderInfo, baseline ->
                                  TextChunkLocationImp(baseline.startPoint, baseline.endPoint, renderInfo.singleSpaceWidth)
                              },
                              val fixedColumns: Int? = null,
                              val targetArea: Rectangle? = null,
                              val minWidth: Float = 1f,
                              val maxColumns: Int = 10,
                              val columnStartMarker: String? = null,
                              val columnEndMarker: String? = null
): LocationTextExtractionStrategy(clStrat) {
    private val foundChunks = ArrayList<TextChunk>()

    private var lastTextRenderInfo: TextRenderInfo? = null

    private val filter = targetArea?.let{
        TextRegionEventFilter(it)
    }


    override fun eventOccurred(data: IEventData?, type: EventType?) {
        if (type == EventType.RENDER_TEXT && (filter == null || filter.accept(data, type))) {
            val renderInfo = data as TextRenderInfo
            var segment = renderInfo.baseline
            if (renderInfo.rise != 0f) {
                // remove the rise from the baseline - we do this because the text from a super/subscript render operations should probably be considered as part of the baseline of the text the super/sub is relative to
                val riseOffsetTransform = Matrix(0f, -renderInfo.rise)
                segment = segment.transformBy(riseOffsetTransform)
            }
            val tc = TextChunk(renderInfo.text, clStrat.createLocation(renderInfo, segment))
            foundChunks.add(tc)

            lastTextRenderInfo = renderInfo
        }
    }

    override fun getSupportedEvents(): Set<EventType?>? {
        return null
    }

    override fun getResultantText(): String {
        val textChunks: List<TextChunk> = ArrayList(sortIntoColumns(foundChunks))
        val sb = StringBuilder()
        var lastChunk: TextChunk? = null
        for (chunk in textChunks) {
            if (lastChunk == null) {
                sb.append(chunk.text)
            } else {
                if (chunk.location.sameLine(lastChunk.location)) {
                    // we only insert a blank space if the trailing character of the previous string wasn't a space, and the leading character of the current string isn't a space
                    if (isChunkAtWordBoundary(chunk, lastChunk) && chunk.text.startsWith(' ') && lastChunk.text.endsWith(' ')
                    ) {
                        sb.append(' ')
                    }
                    sb.append(chunk.text)
                } else {
                    sb.append('\n')
                    sb.append(chunk.text)
                }
            }
            lastChunk = chunk
        }
        return sb.toString()
    }

    /**
     * Sort text first by column, then by position in column
     */
    private fun sortIntoColumns(chunks: Collection<TextChunk>): List<TextChunk>{
        // these are for coordinates in Vector
        val x = 0
        val y = 1
        val z = 2

        // This will hold all the TextChunks, by column it is found in.
        val unsortedColumns = ArrayList<ArrayList<TextChunk>>()


        val mostLeft = targetArea?.left ?: chunks.minByOrNull { it.location.startLocation[x] }?.location?.startLocation?.get(x) ?: return emptyList() // if no chunks, en empty list is all we get
        val mostRight = targetArea?.right ?: chunks.maxByOrNull { it.location.endLocation[x] }?.location?.startLocation?.get(x) ?: return emptyList()

        // Put the chunks in their unsorted column
        if (fixedColumns == null){
            TODO("Maak die hele zooi")
        }
        else{
            //Make lists for every column
            repeat(fixedColumns){
                unsortedColumns.add(ArrayList())
            }
            val pageWidth = mostRight - mostLeft
            val columnWidth = pageWidth/fixedColumns
            chunks.forEach{
                val startPos = it.location.startLocation[x] - mostLeft
                unsortedColumns[(startPos/columnWidth).toInt()].add(it)
            }
        }

        //sort columns first by x then by y (so y is ordered and same y is sorted by x)
        unsortedColumns.map{ it.sortBy { chunk -> chunk.location.startLocation[x] }}
        return unsortedColumns.map{it.sortedByDescending { chunk -> chunk.location.startLocation[y] }}.flatten()
    }

}