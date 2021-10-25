import data.Answer
import data.Question
import parsing.PdfToTextParser
import java.io.File
import java.lang.Exception

class Main {
    companion object {
        const val PATH = "c:\\temp\\qq\\"
        const val FILENAME = "oma"
        const val SUFFIX_IN = ".pdf"
        const val SUFFIX_OUT = ".json"


        @JvmStatic
        fun main(args: Array<String>) {
            val pages = PdfToTextParser(PATH + FILENAME + SUFFIX_IN).parsePages().filter {it.isRelevant}

            val qmap = hashMapOf<String, HashMap<Int, Question>>()
            pages.filter{it.isQuestions}.forEach{ page ->
                (page.questions() ?: emptyList()).forEach { q ->
                    if (qmap[page.topic] == null) qmap[page.topic!!] = hashMapOf()
                    qmap[page.topic]!![q.number] = q
                }
            }
            pages.filter{it.isAnswers}.forEach {
                it.answers()?.let{ aa ->
                    aa.forEach { a ->
                        try{
                            val probableTopic = findProbableTopic(qmap, a)
                            qmap[probableTopic]!![a.number]!!.correctAnswer = a.answer // this should crash because things will be fucked up if that is null
                        } catch(e: Exception){
                            println("answer was $a")
                            println("exception ${e.printStackTrace()}")
                            error ("ded")
                        }
                    }
                }
            }
            /*
            qmap.keys.forEach {
                println("Topic: $it")
                (qmap[it]?.keys ?: emptyList()).forEach{ k ->
                    println(qmap[it]!![k])
                    println(".")
                }
            }
            */
            val allQuestions = qmap.values.map{it.values}.flatten()
            val json = allQuestions.joinToString(",\n", prefix = "{\n\"vragen\":[\n", postfix = "\n]\n}") { it.asJson() }

            File(PATH + FILENAME + SUFFIX_OUT).writeText(json)

            /*
            pages.filter{it.isAnswers}.forEach {
                println(it)
                //println("********** CONTENT **********\n${it.content?.replace("\t", "[TAB]\t")}\n*****************************\n")
                println("********** ANSWERS **********\n${it.answers()?.joinToString("\n")}\n*****************************\n")
            }
            */
        }

        private fun findProbableTopic(map: Map<String, Map<Int, Question>>, a: Answer): String{
            val answerKey = a.topic?.replace(" RG", " REFERENCE GUIDE")
            return map.keys.firstOrNull { key -> answerKey != null && (answerKey.contains(key, ignoreCase = true) || key.contains(answerKey, ignoreCase = true))}
                ?: map.keys.first()
        }
    }
}