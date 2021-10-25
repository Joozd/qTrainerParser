package data

import extensions.indicesOf
import extensions.removeDoubleSpaces
import extensions.removeSpacesBeforePeriod
import java.util.*

class ParsedPage(val issue: String,
                 val questionnaireTitle: String?,
                 val questionnaireChapter: String?,
                 val pageTitle: String?,
                 val pageChapter: String?,
                 val content: String?,
                 val topic: String?) {

    private val contentLines: List<String>? get() = content?.lines()

    /**
     * A page is relevant if it's chapter is not UPDATE_LOG, INTRODUCTION or APPENDICES
     * (so questions and answers are left)
     */
    val isRelevant: Boolean = listOf(CONTENTS, INTRODUCTION, APPENDICES).none{
        pageTitle?.contains(it, ignoreCase = true) ?: false
    }

    /**
     * This page contains answers
     */
    val isAnswers: Boolean = pageTitle?.contains(ANSWERS, ignoreCase = true) ?: false

    /**
     * This page contains questions
     */
    val isQuestions: Boolean = isRelevant && !isAnswers

    /**
     * Get questions if this is a page with questions
     */
    fun questions(): List<Question>? = if (isQuestions) findQuestions() else null

    /**
     * get answers if this is a page with answers
     */
    fun answers(): List<Answer>? = if (isAnswers) findAnswers() else null

    /**
     * Change the content of this page (because it had to be parsed in another way for example)
     */
    fun withContent(newContent: String): ParsedPage {
        return ParsedPage(issue, questionnaireTitle, questionnaireChapter, pageTitle, pageChapter, newContent, topic)
    }

    override fun toString() =
            "chapter:       $questionnaireChapter\n" +
            "title:         $questionnaireTitle\n" +
            "page chapter:  $pageChapter\n" +
            "page title:    $pageTitle\n" +
            "topic:         $topic\n" +
            "type           ${when{
                isQuestions -> "Questions"
                isAnswers -> "Answers"
                else -> "Irellevant"
            }}" + "\n" +
            "questions:\n${questions()?.joinToString("\n\n")}\n"
            //"content:\n$content"

    private fun findQuestions(): List<Question>{
        val questionRegex = """(\d+)\.(.+)""".toRegex()
        val ll = LinkedList(contentLines ?: emptyList<String>())

        //remove top lines untill only first line matches [questionRegex]
        //if first line is a question, second line will be an answer
        while (ll.size > 1 && ll[1].matches(questionRegex)){
            ll.removeFirst()
        }
        val questions = LinkedList(ll.filter { it matches questionRegex })
        return questions.map{
            val lines = LinkedList(listOf(it))
            ll.removeFirst()
            while (ll.isNotEmpty() && ll[0] !in questions){
                lines.add(ll.removeFirst())
            }
            Question.of(lines, topic ?: "")
        }.filterNotNull()
    }

    private fun findAnswers(): List<Answer>{
        val foundAnswers = ArrayList<Answer?>()
        // clean up lines:
        // remove spaces before periods
        // remove double spaces
        // remove lines starting with something other than "Reference" or a digit as they are reference overflows that are not needed here
        val lines = contentLines?.let { cl ->
             cl.map { it.trim().removeDoubleSpaces().removeSpacesBeforePeriod() }
                 .filter { (it.firstOrNull() ?: ' ').isDigit() || it.startsWith("Reference", ignoreCase = true)}
        } ?: return emptyList()
        var currentTopic: String? = null

        //A setries of answers is in the following format in the files:
        // 1 or more lines with topic
        // \s*Reference\s*
        // lines with anwsers
        val referenceLines = lines.indicesOf { it.equals("Reference", ignoreCase = true) }

        //println("KOEKJESBOOT")
        //println(lines.joinToString("\n"))
        referenceLines.forEach { refLineIndex ->
            var linesAway = 0

            // Get the topic of these answers by going up untill we find a line that is an answer
            var topic: String? = null
            //println("reflineIndex: $refLineIndex // linesAway: $linesAway")
            while (refLineIndex - ++linesAway >= 0 && Answer.ofLine(lines[refLineIndex - linesAway]) == null){
                if (topic?.firstOrNull()?.isDigit() != true) topic = lines[refLineIndex - linesAway] + " " + (topic ?: "")
            }
            topic = topic?.trimWithNumbers()
            linesAway = 0
            // get questions
            while (refLineIndex + ++ linesAway in lines.indices && !lines[refLineIndex + linesAway].equals("Reference", ignoreCase = true)){
                foundAnswers.add(Answer.ofLine(lines[refLineIndex + linesAway], topic))
            }
        }

        return foundAnswers.filterNotNull()
    }

    companion object{
        /**
         * Build a data.ParsedPage object from raw data
         */
        fun of(rawData: String, previousTopic: String? = null) : ParsedPage {
            val lines = rawData
                .lines()
                .filter{ it.isNotBlank() }

            //println("xxxxxxxxxxxxxx")
            //println("lines:\n${lines.joinToString("\n")}")

            val titleRegex = """([0-9.]+)\s+(.+)""".toRegex()
            val backupChapterRegex = """\d\.\d""".toRegex()

            /**
             * Title of this questionnaire, null if empty page
             */
            val questionnaireTitle = lines.firstOrNull()?.let{ firstLine ->
                titleRegex.find(firstLine)?.groupValues?.last()
            } ?: "NO TITLE FOUND"

            /**
             * Chapter number of this questionnaire
             */
            val questionnaireChapter = lines.firstOrNull()?.let{ firstLine ->
                titleRegex.find(firstLine)?.let{ result ->
                    result.groupValues[1]
                }
                    ?: backupChapterRegex.find(lines.firstOrNull{ backupChapterRegex in it} ?: "")?.value
            }

            val pageTitleRegex = """(${questionnaireChapter}[0-9.]+)\s+(.+)""".toRegex()
            val pageTitleLine = lines.firstOrNull{ pageTitleRegex in it}

            /**
             * Title of this page
             */
            val pageTitle = pageTitleRegex.find(pageTitleLine ?: "")?.groupValues?.last()?.trim()
                ?: "NO TITLE FOUND"

            /**
             * Chapter of this page
             */
            val pageChapter = pageTitleRegex.find(pageTitleLine ?: "")?.groupValues?.get(1)?.trim()

            val joinedLines = lines.joinToString("\n")
            val contentRegex = """$pageChapter\s+$pageTitle(.*)$pageChapter""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val backupContentRegex = """$pageChapter\s+$pageTitle(.*)ISSUE\s+\d+""".toRegex(RegexOption.DOT_MATCHES_ALL)

            val contentLines = contentRegex.find(joinedLines)?.groupValues?.get(1)?.trim()?.lines()
                ?: backupContentRegex.find(joinedLines)?.groupValues?.get(1)?.trim()?.lines()?.dropLast(1)
            /**
             * Content of this page as one big string, without header and footer
             */
            val content = contentLines?.joinToString("\n")

            //fun getQuestions(previousQuestionNumber: Int = 0)

            val topic = findTopic(contentLines) ?: previousTopic ?: "NO TOPIC"

            val issueRegex = """ISSUE (\d+)""".toRegex()

            val issue = issueRegex.find(joinedLines)?.groupValues?.get(1) ?: "-1"

            return ParsedPage(issue, questionnaireTitle, questionnaireChapter, pageTitle, pageChapter, content, topic)
        }

        private fun findTopic(contentLines: List<String>?): String?{
            val questionRegex = """(\d+)\.(.+)""".toRegex()
            val ll = LinkedList(contentLines ?: emptyList<String>())

            //remove top lines untill only first or first two lines match [questionRegex]
            //if first line is a question, second line will be an answer
            while (ll.size > 2 && ll[2].matches(questionRegex)){
                ll.removeFirst()
            }
            // If the first line on a page doesn't look like a question, it is a topic
            if (!(ll.firstOrNull() ?: "").matches(questionRegex)) return ll.firstOrNull()

            //if second line also looks like a question, first was topic
            if (ll.size > 1 && ll[1].matches(questionRegex)) return questionRegex.find(ll.firstOrNull() ?: "")?.groupValues?.get(2)?.trimWithNumbers()

            //If neither is the case, no topic on this page.
            return null
        }

        /**
         * Remove leading spaces and numbers
         */
        private fun String.trimWithNumbers(): String = if (this.trim().first().isDigit()) this.trim().drop(1).drop(1).trimWithNumbers() else this.trim()

        const val CONTENTS = "contents"
        const val INTRODUCTION = "introduction"
        const val APPENDICES = "append"

        const val ANSWERS = "answer"

    }
}