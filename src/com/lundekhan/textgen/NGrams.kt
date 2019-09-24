package com.lundekhan.textgen

import edu.stanford.nlp.process.PTBTokenizer
import java.io.BufferedReader
import kotlin.random.Random
import edu.stanford.nlp.process.CoreLabelTokenFactory
import java.io.FileReader
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.streams.toList
import kotlin.system.measureTimeMillis

enum class NGramType {
    STRING,
    CHARACTER
}

fun tokenizeWords(reader: Reader): Sequence<String> {
    return PTBTokenizer(reader, CoreLabelTokenFactory(), "tokenizeNLs=true")
        .asSequence()
        .map { it.value() }
}

fun ngrams(tokens: List<String>, n: Int, padStart: Char? = null, padEnd: Char? = null): List<List<String>> {
    val padLeft = if (padStart != null) List(n) { padStart.toString() } else listOf()
    val padRight = if (padEnd != null) listOf(padEnd.toString()) else listOf()

    return (padLeft + tokens + padRight).windowed(n)
}

typealias InternalLanguageModel = Map<String, Map<String, Double>>

private fun List<String>.createKey(): String = this.joinToString("-")
private fun List<String>.groupByAverageTotal(): Map<String, Double> {
    val incr = 1.0 / this.size
    val unsafeGroupMap = mutableMapOf<String, Double>()
    this.forEach { word -> unsafeGroupMap[word] = unsafeGroupMap.getOrDefault(word, 0.0) + incr }

    return unsafeGroupMap
}

class LanguageModel(val n: Int, val fileName: String) {
    private val paddingStart = '\u0002'
    private val paddingEnd = '\u0003'
    private val random = Random
    private val missingMap = mapOf("<UNK>" to 1.0)
    private val internalLanguageModel: InternalLanguageModel by lazy { createLanguageModel() }
    private val internalWordLanguageModel: InternalLanguageModel by lazy { createWordLanguageModel() }

    private fun readFileAsLinesUsingGetResourceAsStream(fileName: String): BufferedReader =
        this::class.java.getResourceAsStream(fileName).bufferedReader()

    private fun getFilePath(fileName: String): String =
        this::class.java.getResource(fileName).path

    private fun createWordLanguageModel(): InternalLanguageModel =
        ngrams(tokenizeWords(FileReader(getFilePath(fileName))).toList(), n)
            .groupBy(
                keySelector = { it.dropLast(1).createKey() },
                valueTransform = { it.last() }
            )
            .mapValues { (_, value) -> value.groupByAverageTotal() }

    private fun createLanguageModel(): InternalLanguageModel {
        return Files.lines(Paths.get(javaClass.getResource(fileName).path))
            .parallel()
            .flatMap { (it + "\n").tokenizeChars().stream() }
            .toList()
            .let { ngrams(it, n) }
            .groupBy(
                keySelector = { it.dropLast(1).createKey() },
                valueTransform = { it.last() }
            )
            .mapValues { (_, value) -> value.groupByAverageTotal() }

        //return readFileAsLinesUsingGetResourceAsStream(fileName)
        //    .useLines { lines -> // Lines are not merged with eachother.. That ruins A LOT.
        //        lines
        //            .windowed(2)
        //            .flatMap { windowedLine ->
        //                val line = windowedLine.first() + "\n" + windowedLine.last().take(n)
        //                val tokens = line.tokenizeChars()
        //                ngrams(tokens, n).asSequence()
        //            }
        //            .groupBy(
        //                keySelector = { it.dropLast(1).createKey() },
        //                valueTransform = { it.last() }
        //            )
        //            .mapValues { (_, value) -> value.groupByAverageTotal() }
        //    }
    }


    fun generateOneGram(history: List<String>, temperature: Double): String { // This one is the same..!
        val hist = history.takeLast(n - 1).createKey()
        println(hist)
        val distr = internalLanguageModel.getOrDefault(hist, missingMap)
        println(distr)
        var x = random.nextDouble()
        return distr
            .entries
            .dropWhile { (_, value) -> x -= value; return@dropWhile x > 0 }
            .first().key
    }

    fun generateTextByChar(history: String, size: Int = 250, temperature: Double = 0.0): String {
        val hist = history.toCharArray().map { it.toString() }
        return (0..size)
            .fold(hist) { acc, _ -> acc + generateOneGram(acc, temperature) }
            .joinToString("")
    }

    fun generateTextByWord(history: String, size: Int = 250, temperature: Double = 0.0): String = ""

    object Hej {
        @JvmStatic
        fun main(args: Array<String>) {
            val lm = LanguageModel(5, "/texts/shakespeare.txt")
            println("Avg time")
            print(measureTimeMillis {
                println(lm.generateTextByChar("who is the", temperature = 0.5))
            } / (11))
            println(" ms")
        }
    }
}