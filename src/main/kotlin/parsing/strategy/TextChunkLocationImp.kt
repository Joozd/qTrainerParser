package parsing.strategy

import com.itextpdf.kernel.geom.LineSegment
import com.itextpdf.kernel.geom.Vector
import com.itextpdf.kernel.pdf.canvas.parser.listener.ITextChunkLocation

class TextChunkLocationImp(
    /**
     * The starting location of the chunk.
     */
    private val startLocation: Vector,
    /**
     * The ending location of the chunk.
     */
    private val endLocation: Vector,
    /**
     * The width of a single space character in the font of the chunk.
     */
    private val charSpaceWidth: Float
) :
    ITextChunkLocation {
    /**
     * Unit vector in the orientation of the chunk.
     */
    private val orientationVector: Vector

    /**
     * The orientation as a scalar for quick sorting.
     */
    private val orientationMagnitude: Int

    /**
     * Perpendicular distance to the orientation unit vector (i.e. the Y position in an unrotated coordinate system).
     * We round to the nearest integer to handle the fuzziness of comparing floats.
     */
    private val distPerpendicular: Int

    /**
     * Distance of the start of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system).
     */
    private val distParallelStart: Float

    /**
     * Distance of the end of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system).
     */
    private val distParallelEnd: Float
    override fun orientationMagnitude(): Int {
        return orientationMagnitude
    }

    override fun distPerpendicular(): Int {
        return distPerpendicular
    }

    override fun distParallelStart(): Float {
        return distParallelStart
    }

    override fun distParallelEnd(): Float {
        return distParallelEnd
    }

    /**
     * @return the start location of the text
     */
    override fun getStartLocation(): Vector {
        return startLocation
    }

    /**
     * @return the end location of the text
     */
    override fun getEndLocation(): Vector {
        return endLocation
    }

    /**
     * @return the width of a single space character as rendered by this chunk
     */
    override fun getCharSpaceWidth(): Float {
        return charSpaceWidth
    }

    /**
     * @param as the location to compare to
     * @return true is this location is on the the same line as the other
     */
    override fun sameLine(`as`: ITextChunkLocation): Boolean {
        if (orientationMagnitude() != `as`.orientationMagnitude()) {
            return false
        }
        val distPerpendicularDiff = (distPerpendicular() - `as`.distPerpendicular()).toFloat()
        if (distPerpendicularDiff == 0f) {
            return true
        }
        val mySegment = LineSegment(startLocation, endLocation)
        val otherSegment = LineSegment(`as`.startLocation, `as`.endLocation)
        return Math.abs(distPerpendicularDiff) <= DIACRITICAL_MARKS_ALLOWED_VERTICAL_DEVIATION && (mySegment.length == 0f || otherSegment.length == 0f)
    }

    /**
     * Computes the distance between the end of 'other' and the beginning of this chunk
     * in the direction of this chunk's orientation vector.  Note that it's a bad idea
     * to call this for chunks that aren't on the same line and orientation, but we don't
     * explicitly check for that condition for performance reasons.
     *
     * @param other
     * @return the number of spaces between the end of 'other' and the beginning of this chunk
     */
    override fun distanceFromEndOf(other: ITextChunkLocation): Float {
        return distParallelStart() - other.distParallelEnd()
    }

    override fun isAtWordBoundary(previous: ITextChunkLocation): Boolean {
        // In case a text chunk is of zero length, this probably means this is a mark character,
        // and we do not actually want to insert a space in such case
        if (startLocation == endLocation || previous.endLocation == previous.startLocation) {
            return false
        }
        var dist = distanceFromEndOf(previous)
        if (dist < 0) {
            dist = previous.distanceFromEndOf(this)

            //The situation when the chunks intersect. We don't need to add space in this case
            if (dist < 0) {
                return false
            }
        }
        return dist > getCharSpaceWidth() / 2.0f
    }

    companion object {
        private const val DIACRITICAL_MARKS_ALLOWED_VERTICAL_DEVIATION = 2f
        fun containsMark(baseLocation: ITextChunkLocation, markLocation: ITextChunkLocation): Boolean {
            return baseLocation.startLocation[Vector.I1] <= markLocation.startLocation[Vector.I1] && baseLocation.endLocation[Vector.I1] >= markLocation.endLocation[Vector.I1] && Math.abs(
                baseLocation.distPerpendicular() - markLocation.distPerpendicular()
            ) <= DIACRITICAL_MARKS_ALLOWED_VERTICAL_DEVIATION
        }
    }

    init {
        var oVector = endLocation.subtract(startLocation)
        if (oVector.length() == 0f) {
            oVector = Vector(1f, 0f, 0f)
        }
        orientationVector = oVector.normalize()
        orientationMagnitude = (Math.atan2(
            orientationVector[Vector.I2].toDouble(),
            orientationVector[Vector.I1].toDouble()
        ) * 1000).toInt()

        // see http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
        // the two vectors we are crossing are in the same plane, so the result will be purely
        // in the z-axis (out of plane) direction, so we just take the I3 component of the result
        val origin = Vector(0f, 0f, 1f)
        distPerpendicular = startLocation.subtract(origin).cross(orientationVector)[Vector.I3].toInt()
        distParallelStart = orientationVector.dot(startLocation)
        distParallelEnd = orientationVector.dot(endLocation)
    }
}
