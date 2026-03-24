package io.ksmt.solver.noodler.automaton

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseAutomataTest {

    @Test
    fun testEpsilonAcceptsOnlyEmptyWord() {
        val base = BaseAutomata(alphabet = listOf('a'))
        val epsilon = base.epsilon()

        assertTrue(epsilon.check(""))
        assertFalse(epsilon.check("a"))
        assertFalse(epsilon.check("aa"))
    }

    @Test
    fun testAllCharAcceptsSingleAlphabetChar() {
        val base = BaseAutomata(alphabet = listOf('a', 'b'))
        val allChar = base.allChar()

        assertTrue(allChar.check("a"))
        assertTrue(allChar.check("b"))
        assertFalse(allChar.check(""))
        assertFalse(allChar.check("ab"))
        assertFalse(allChar.check("c"))
    }

    @Test
    fun testAllWordAcceptsAnyWordOverAlphabet() {
        val base = BaseAutomata(alphabet = listOf('a', 'b'))
        val allWord = base.allWord()

        assertTrue(allWord.check(""))
        assertTrue(allWord.check("a"))
        assertTrue(allWord.check("ba"))
        assertTrue(allWord.check("abba"))
        assertFalse(allWord.check("c"))
    }

    @Test
    fun testWordAcceptsOnlyExactWord() {
        val base = BaseAutomata(alphabet = listOf('a', 'b'))
        val word = base.word("ab")

        assertTrue(word.check("ab"))
        assertFalse(word.check(""))
        assertFalse(word.check("a"))
        assertFalse(word.check("aba"))
        assertFalse(word.check("bb"))
    }

    @Test
    fun testRangeAcceptsOnlyCharsInRange() {
        val base = BaseAutomata(alphabet = listOf('a', 'b', 'c', 'd'))
        val range = base.range('b', 'c')

        assertTrue(range.check("b"))
        assertTrue(range.check("c"))
        assertFalse(range.check("a"))
        assertFalse(range.check("d"))
        assertFalse(range.check("bc"))
    }
}
