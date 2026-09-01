package com.example.vaultcalc.download.resolver

import org.junit.Test
import kotlin.system.measureTimeMillis

class HtmlMetadataStrategyBenchmarkTest {

    @Test
    fun benchmarkRegex() {
        val iterations = 100000
        val line = "<meta property=\"og:video:url\" content=\"https://example.com/video.mp4\">"

        val unoptimizedTime = measureTimeMillis {
            for (i in 0 until iterations) {
                val ogVideoRegex = Regex("<meta\\s+property=\"og:video:url\"\\s+content=\"([^\"]+)\"")
                val ogVideoAltRegex = Regex("<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"")
                val twitterPlayerStream = Regex("<meta\\s+name=\"twitter:player:stream\"\\s+content=\"([^\"]+)\"")
                val titleRegex = Regex("<title>([^<]+)</title>")

                titleRegex.find(line)
                listOf(ogVideoRegex, ogVideoAltRegex, twitterPlayerStream).forEach { regex ->
                    regex.find(line)
                }
            }
        }

        val ogVideoRegex = Regex("<meta\\s+property=\"og:video:url\"\\s+content=\"([^\"]+)\"")
        val ogVideoAltRegex = Regex("<meta\\s+property=\"og:video\"\\s+content=\"([^\"]+)\"")
        val twitterPlayerStream = Regex("<meta\\s+name=\"twitter:player:stream\"\\s+content=\"([^\"]+)\"")
        val titleRegex = Regex("<title>([^<]+)</title>")
        val mediaRegexes = listOf(ogVideoRegex, ogVideoAltRegex, twitterPlayerStream)

        val optimizedTime = measureTimeMillis {
            for (i in 0 until iterations) {
                titleRegex.find(line)
                mediaRegexes.forEach { regex ->
                    regex.find(line)
                }
            }
        }

        println("BENCHMARK_RESULT: Unoptimized: ${unoptimizedTime}ms")
        println("BENCHMARK_RESULT: Optimized: ${optimizedTime}ms")
        val diff = unoptimizedTime - optimizedTime
        val pct = (diff.toDouble() / unoptimizedTime) * 100
        println("BENCHMARK_RESULT: Improvement: %.2f%%".format(pct))
    }
}
