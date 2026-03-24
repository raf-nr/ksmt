package io.ksmt.solver.noodler.automaton

import org.junit.jupiter.api.Assumptions
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AutomatonPictureTest {

    @Test
    fun testToDotProducesDeterministicGraphvizDescription() {
        val terminal = NFANode(true, mutableMapOf())
        val start = NFANode(false, mutableMapOf())
        start.put('a', terminal)
        start.put(EPS, terminal)

        val nfa = NFA(start, alphabet = listOf('a'))

        val expected = """
            digraph Automaton {
                rankdir=LR;
                start [shape=point];
                start -> q0;
                q0 [shape=circle, label="q0"];
                q1 [shape=doublecircle, label="q1"];
                q0 -> q1 [label="eps, a"];
            }
        """.trimIndent()

        assertEquals(expected, nfa.toDot().trim())
    }

    @Test
    fun testPictureCreatesSvgFile() {
        Assumptions.assumeTrue(isGraphvizAvailable(), "Graphviz 'dot' is not installed")

        val terminal = NFANode(true, mutableMapOf())
        val start = NFANode(false, mutableMapOf())
        start.put('a', terminal)
        start.put(EPS, terminal)

        val nfa = NFA(start, alphabet = listOf('a'))
        val tempDir = Files.createTempDirectory("automaton-picture")

        try {
            val output = tempDir.resolve("nfa.svg")
            nfa.picture(output.toString())

            assertTrue(Files.exists(output))
            assertTrue(Files.size(output) > 0)

            val svg = String(Files.readAllBytes(output), Charsets.UTF_8)
            assertTrue(svg.contains("q0"))
            assertTrue(svg.contains("q1"))
            assertTrue(svg.contains("eps, a"))
        } finally {
            tempDir.toFile().deleteRecursively()
        }
    }

    @Test
    fun testPictureRequiresFilenameExtension() {
        val nfa = NFA(NFANode(true, mutableMapOf()), alphabet = emptyList())

        val error = assertFailsWith<IllegalArgumentException> {
            nfa.picture("automaton")
        }

        assertTrue(error.message.orEmpty().contains("extension"))
    }

    private fun isGraphvizAvailable(): Boolean = runCatching {
        val process = ProcessBuilder("dot", "-V")
            .redirectErrorStream(true)
            .start()

        process.outputStream.close()
        process.inputStream.use { it.readBytes() }
        process.waitFor() == 0
    }.getOrDefault(false)
}
