package io.ksmt.solver.noodler.automaton

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NFAExtendedTest {

    @Test
    fun testUnionAcceptsEitherLanguage() {
        val nfaA = literal('a', listOf('a'))
        val nfaB = literal('b', listOf('b'))

        val union = nfaA.union(nfaB)

        assertTrue(union.check("a"))
        assertTrue(union.check("b"))
        assertFalse(union.check(""))
        assertFalse(union.check("ab"))
        assertTrue(union.alphabet.contains('a'))
        assertTrue(union.alphabet.contains('b'))
    }

    @Test
    fun testKleeneClosureAcceptsRepetitionAndEmpty() {
        val nfa = literal('a', listOf('a', 'b'))
        val closure = nfa.kleeneClosure()

        assertTrue(closure.check(""))
        assertTrue(closure.check("a"))
        assertTrue(closure.check("aa"))
        assertFalse(closure.check("b"))
    }

    @Test
    fun testPowerBuildsExactRepetition() {
        val nfa = literal('a', listOf('a'))

        val pow0 = nfa.power(0)
        assertTrue(pow0.check(""))
        assertFalse(pow0.check("a"))

        val pow1 = nfa.power(1)
        assertTrue(pow1.check("a"))
        assertFalse(pow1.check(""))

        val pow2 = nfa.power(2)
        assertTrue(pow2.check("aa"))
        assertFalse(pow2.check("a"))
        assertFalse(pow2.check("aaa"))
    }

    @Test
    fun testLoopAcceptsRangeInclusive() {
        val nfa = literal('a', listOf('a'))
        val loop = nfa.loop(1, 3)

        assertTrue(loop.check("a"))
        assertTrue(loop.check("aa"))
        assertTrue(loop.check("aaa"))
        assertFalse(loop.check(""))
        assertFalse(loop.check("aaaa"))
    }

    @Test
    fun testLoopFromZeroIncludesEmpty() {
        val nfa = literal('a', listOf('a'))
        val loop = nfa.loop(0, 0)

        assertTrue(loop.check(""))
        assertFalse(loop.check("a"))
    }

    @Test
    fun testDifferenceRemovesLanguage() {
        val base = literal('a', listOf('a'))
        val star = base.kleeneClosure()

        val diff = star.difference(base)

        assertTrue(diff.check(""))
        assertFalse(diff.check("a"))
        assertTrue(diff.check("aa"))
        assertTrue(diff.check("aaa"))
    }

    @Test
    fun testDifferenceUsesAlphabetUnion() {
        val a = literal('a', listOf('a'))
        val b = literal('b', listOf('b'))

        val diff = a.difference(b)

        assertTrue(diff.check("a"))
        assertFalse(diff.check(""))
        assertFalse(diff.check("b"))
    }

    @Test
    fun testPowerRejectsNegative() {
        val nfa = literal('a', listOf('a'))
        assertFailsWith<IllegalArgumentException> { nfa.power(-1) }
    }

    @Test
    fun testLoopValidatesBounds() {
        val nfa = literal('a', listOf('a'))
        assertFailsWith<IllegalArgumentException> { nfa.loop(-1, 2) }
        assertFailsWith<IllegalArgumentException> { nfa.loop(2, 1) }
    }

    private fun literal(ch: Char, alphabet: List<Char>): NFA {
        val start = node(terminal = false)
        val terminal = node(terminal = true)
        start.put(ch, terminal)
        return NFA(start, alphabet)
    }

    private fun node(terminal: Boolean): NFANode {
        return NFANode(terminal, mutableMapOf())
    }
}
