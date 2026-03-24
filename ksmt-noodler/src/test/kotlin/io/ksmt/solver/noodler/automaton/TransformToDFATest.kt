package io.ksmt.solver.noodler.automaton

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransformToDFATest {

    @Test
    fun testTransformToDfaUsesEpsilonClosureOnStart() {
        val terminal = node(terminal = true)
        val start = node(terminal = false)
        start.put(EPS, terminal)

        val nfa = NFA(start = start, alphabet = listOf('a'))
        val dfa = transformToDFA(nfa)

        assertTrue(dfa.check(""))
        assertFalse(dfa.check("a"))
    }

    @Test
    fun testTransformToDfaUsesEpsilonClosureAfterMove() {
        val terminal = node(terminal = true)
        val mid = node(terminal = false)
        val start = node(terminal = false)
        start.put(EPS, mid)
        mid.put('a', terminal)

        val nfa = NFA(start = start, alphabet = listOf('a'))
        val dfa = transformToDFA(nfa)

        assertTrue(dfa.check("a"))
        assertFalse(dfa.check(""))
        assertFalse(dfa.check("b"))
    }

    @Test
    fun testTransformToDfaPreservesNondeterministicChoices() {
        val acceptA = node(terminal = true)
        val mid = node(terminal = false)
        val acceptAB = node(terminal = true)
        val start = node(terminal = false)
        start.put('a', acceptA)
        start.put('a', mid)
        mid.put('b', acceptAB)

        val nfa = NFA(start = start, alphabet = listOf('a', 'b'))
        val dfa = transformToDFA(nfa)

        val samples = listOf("", "a", "ab", "b", "aa", "abb")
        for (word in samples) {
            assertEquals(nfa.check(word), dfa.check(word), "Mismatch on '$word'")
        }
    }

    @Test
    fun testTransformToDfaDropsEpsilonFromAlphabet() {
        val terminal = node(terminal = true)
        val start = node(terminal = false)
        start.put(EPS, terminal)
        start.put('a', terminal)

        val nfa = NFA(start = start, alphabet = listOf(EPS, 'a'))
        val dfa = transformToDFA(nfa)

        assertFalse(dfa.alphabet.contains(EPS))
        assertTrue(dfa.alphabet.contains('a'))
    }

    private fun node(terminal: Boolean): NFANode {
        return NFANode(terminal, mutableMapOf())
    }
}
