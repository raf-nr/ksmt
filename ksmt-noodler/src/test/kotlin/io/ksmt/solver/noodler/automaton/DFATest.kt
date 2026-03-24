package io.ksmt.solver.noodler.automaton

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DFATest {

    @Test
    fun testComplementHandlesMissingTransitions() {
        val start = node(terminal = false)
        val accept = node(terminal = true)
        start.put('a', accept)
        accept.put('a', accept)

        val dfa = DFA(start, alphabet = listOf('a', 'b'))
        val complement = dfa.complement()

        val samples = listOf("", "a", "aa", "b", "ab", "ba")
        for (word in samples) {
            assertEquals(!dfa.check(word), complement.check(word), "Mismatch on '$word'")
        }
    }

    @Test
    fun testAsNfaPreservesLanguage() {
        val start = node(terminal = false)
        val accept = node(terminal = true)

        start.put('a', accept)
        start.put('b', start)
        accept.put('a', accept)
        accept.put('b', start)

        val dfa = DFA(start, alphabet = listOf('a', 'b'))
        val nfa = dfa.asNFA()

        val samples = listOf("", "a", "b", "ba", "ab", "bb", "aba", "abba")
        for (word in samples) {
            assertEquals(dfa.check(word), nfa.check(word), "Mismatch on '$word'")
        }
    }

    @Test
    fun testComplementOfUniversalLanguage() {
        val start = node(terminal = true)
        start.put('a', start)

        val dfa = DFA(start, alphabet = listOf('a'))
        val complement = dfa.complement()

        val samples = listOf("", "a", "aa", "aaa")
        for (word in samples) {
            assertFalse(complement.check(word), "Expected complement to reject '$word'")
        }
    }

    private fun node(terminal: Boolean): DFANode {
        return DFANode(terminal, mutableMapOf())
    }
}
