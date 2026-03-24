package io.ksmt.solver.noodler.automaton

import kotlin.test.Test
import kotlin.test.assertEquals

class DFAMinimizeTest {

    @Test
    fun testMinimizeMergesEquivalentTerminalStates() {
        val start = node(terminal = false)
        val terminalA = node(terminal = true)
        val terminalB = node(terminal = true)

        start.put('a', terminalA)
        start.put('b', terminalB)
        terminalA.put('a', terminalA)
        terminalA.put('b', terminalA)
        terminalB.put('a', terminalB)
        terminalB.put('b', terminalB)

        val dfa = DFA(start, alphabet = listOf('a', 'b'))
        val minimized = dfa.minimize()

        assertEquals(3, listNodes(dfa.start, { true }).size)
        assertEquals(2, listNodes(minimized.start, { true }).size)

        val samples = listOf("", "a", "b", "ab", "ba", "bbb")
        for (word in samples) {
            assertEquals(dfa.check(word), minimized.check(word), "Mismatch on '$word'")
        }
    }

    @Test
    fun testMinimizeKeepsDistinctNonEquivalentStates() {
        val start = node(terminal = false)
        val terminalA = node(terminal = true)
        val terminalB = node(terminal = true)
        val sink = node(terminal = false)

        start.put('a', terminalA)
        start.put('b', terminalB)
        terminalA.put('a', terminalA)
        terminalA.put('b', sink)
        terminalB.put('a', sink)
        terminalB.put('b', terminalB)
        sink.put('a', sink)
        sink.put('b', sink)

        val dfa = DFA(start, alphabet = listOf('a', 'b'))
        val minimized = dfa.minimize()

        assertEquals(4, listNodes(minimized.start, { true }).size)
    }

    private fun node(terminal: Boolean): DFANode {
        return DFANode(terminal, mutableMapOf())
    }
}
