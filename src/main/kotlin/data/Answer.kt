package data

import extensions.removeDoubleSpaces
import extensions.removeSpacesBeforePeriod

/**
 * Not every answer has a topic
 */
class Answer(val number: Int, val answer: Char, val topic: String? = null) {
    override fun toString() = "$topic: $number.\t$answer"

    companion object{
        fun ofLine(line: String, topic: String? = null): Answer? {
            val answerRegex = """(\d+)\.\s([A-Za-z]).*""".toRegex()
            val l = cleanLine(line)
            return answerRegex.find(l)?.groupValues?.let {
                    Answer(it[1].toInt(), it[2].first(), topic)
                }
        }

        /**
         * Clean a line.
         * eg.
         * 1 .        A           14.08.20 +
         * will become
         * 1. A 14.08.20 +
         */
        private fun cleanLine(l: String): String = l.removeDoubleSpaces().removeSpacesBeforePeriod()
    }
}