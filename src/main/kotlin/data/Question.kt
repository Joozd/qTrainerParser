package data

import java.util.*

/**
 *
 */
class Question(val subject: String, val number: Int, val question: String, val answers: List<String>) {
    /**
     * Correct answer, to be set at a later time because it's not parsed with the rest of the question
     */
    var correctAnswer: Char? = null

    /**
     * Available letters for answers (eg 'a', 'b', 'c', 'd' if 4 possible answers)
     */
    val answerLetters = ALPHABET.take(answers.size).toList()

    operator fun get(letter: Char): String?{
        val index = ALPHABET.indexOf(letter)
        return when{
            index == -1 || index >= answers.size -> null
            else -> answers[index]
        }
    }

    override fun toString(): String = "$number. $question\n" + answerLetters.map{ "$it) ${this[it]}"}.joinToString("\n") + "\nCorrect answer: $correctAnswer\n**********"

    fun asJson(): String =
        "{\n" +
        "\"onderwerp\":\"$subject\",\n" +
        "\"vraag\":\"$question\",\n" +
        "\"antwoord\":\"$correctAnswer\",\n" +
        answerLetters.joinToString("") { "\"$it\":\"${this[it]}\",\n" } +
        "\"picture\":\"\"\n" +
        "}"





    companion object{
        private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        private val questionRegex = """(\d+)\.\s*(.+)""".toRegex()
        private val aRegex = """[a-z]\)(.*)""".toRegex()
        fun of(lines: List<String>, topic: String): Question?{
            if (lines.isEmpty()) return null
            val number = lines.firstOrNull()?.let { l1 -> questionRegex.find(l1)?.groupValues?.get(1)?.toInt() } ?: return null
            val qAndAnswers = buildAnswers(lines)
            val question = questionRegex.find(qAndAnswers.firstOrNull() ?: "")?.groupValues?.get(2) ?: return null
            val answers = qAndAnswers.drop(1)
            return Question(topic, number, question, answers)
        }

        private fun buildAnswers(lines: List<String>): List<String>{
            val ll = LinkedList(lines)
            val a = mutableListOf<String?>()
            var currentAnswer: String? = null
            while (ll.isNotEmpty()){
                ll.removeFirst().let{
                    when {
                        it.startsWith(" ") -> {
                            a.add(currentAnswer)
                            currentAnswer = it.drop(1)
                        }
                        it matches aRegex -> {
                            a.add(currentAnswer)
                            currentAnswer = aRegex.find(it)!!.groupValues[1].trim()
                        }
                        else -> currentAnswer = (currentAnswer ?: "") + " ${it.trim()}"
                    }
                }
            }
            a.add(currentAnswer)
            return a.filterNotNull()
        }
    }
}