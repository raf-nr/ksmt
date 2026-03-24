package io.ksmt.solver.noodler.automaton

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.sort.KRegexSort
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RegexToNFATest {

    @Test
    fun testRangeStar() = with(KContext()) {
        val range = mkRegexRange(mkStringLiteral("a"), mkStringLiteral("t"))
        val star = mkRegexStar(range)

        val nfa = fromRegex(star, alphabet = ('a'..'u').toList())

        assertAccepts(nfa, "", "a", "t", "abc", "ttt")
        assertRejects(nfa, "u", "az", "A")
    }

    @Test
    fun testAbPlusC() = with(KContext()) {
        val a = lit("a")
        val b = lit("b")
        val c = lit("c")
        val bPlus = mkRegexCross(b)
        val regex = mkRegexConcat(mkRegexConcat(a, bPlus), c)

        val nfa = fromRegex(regex, alphabet = listOf('a', 'b', 'c'))

        assertAccepts(nfa, "abc", "abbc", "abbbbbc")
        assertRejects(nfa, "ac", "ab", "abcc", "aabc")
    }

    @Test
    fun testXyStarZPlusAbc() = with(KContext()) {
        val x = lit("x")
        val y = lit("y")
        val z = lit("z")
        val dot = mkRegexAllChar()

        val yStar = mkRegexStar(y)
        val zPlus = mkRegexCross(z)
        val abc = mkRegexUnion(mkRegexUnion(lit("a"), lit("b")), lit("c"))

        // xy*.z+(a|b|c)
        val regex = mkRegexConcat(
            mkRegexConcat(
                mkRegexConcat(mkRegexConcat(x, yStar), dot),
                zPlus
            ),
            abc
        )

        val nfa = fromRegex(regex, alphabet = listOf('a', 'b', 'c', 'x', 'y', 'z', 'q'))

        assertAccepts(nfa, "xzza", "xyyzzb", "xyazc", "xqza")
        assertRejects(nfa, "xza", "xyya", "xyzzd", "xqz")
    }

    @Test
    fun testLiteralPupa() = with(KContext()) {
        val regex = lit("pupa")
        val nfa = fromRegex(regex, alphabet = listOf('p', 'u', 'a'))

        assertAccepts(nfa, "pupa")
        assertRejects(nfa, "", "pup", "pupaa")
    }

    @Test
    fun testOperationsCoverage() = with(KContext()) {
        val a = lit("a")
        val b = lit("b")
        val c = lit("c")

        // Epsilon (empty language)
        val empty = fromRegex(mkRegexEpsilon(), alphabet = listOf('a'))
        assertRejects(empty, "", "a")

        // All / AllChar
        val all = fromRegex(mkRegexAll(), alphabet = listOf('a', 'b'))
        assertAccepts(all, "", "a", "ab", "bba")
        assertRejects(all, "c")

        val allChar = fromRegex(mkRegexAllChar(), alphabet = listOf('a', 'b'))
        assertAccepts(allChar, "a", "b")
        assertRejects(allChar, "", "ab", "c")

        // Concat / Union
        val concat = fromRegex(mkRegexConcat(a, b), alphabet = listOf('a', 'b'))
        assertAccepts(concat, "ab")
        assertRejects(concat, "a", "b", "")

        val union = fromRegex(mkRegexUnion(a, b), alphabet = listOf('a', 'b'))
        assertAccepts(union, "a", "b")
        assertRejects(union, "ab", "")

        // Intersection
        val inter = fromRegex(mkRegexIntersection(mkRegexUnion(a, b), mkRegexUnion(b, c)), alphabet = listOf('a', 'b', 'c'))
        assertAccepts(inter, "b")
        assertRejects(inter, "a", "c", "bb")

        // Star / Cross
        val star = fromRegex(mkRegexStar(a), alphabet = listOf('a'))
        assertAccepts(star, "", "a", "aa")
        assertRejects(star, "b")

        val plus = fromRegex(mkRegexCross(a), alphabet = listOf('a'))
        assertAccepts(plus, "a", "aa")
        assertRejects(plus, "")

        // Difference / Complement
        val diff = fromRegex(mkRegexDifference(mkRegexUnion(a, b), b), alphabet = listOf('a', 'b'))
        assertAccepts(diff, "a")
        assertRejects(diff, "b", "ab", "")

        val comp = fromRegex(mkRegexComplement(a), alphabet = listOf('a', 'b'))
        assertAccepts(comp, "", "b", "bb", "ab")
        assertRejects(comp, "a")

        // Option
        val opt = fromRegex(mkRegexOption(a), alphabet = listOf('a'))
        assertAccepts(opt, "", "a")
        assertRejects(opt, "aa", "b")

        // Range
        val range = fromRegex(mkRegexRange(mkStringLiteral("b"), mkStringLiteral("d")), alphabet = listOf('a', 'b', 'c', 'd'))
        assertAccepts(range, "b", "c", "d")
        assertRejects(range, "a", "e", "bc")

        // Power
        val pow = fromRegex(mkRegexPower(3, a), alphabet = listOf('a'))
        assertAccepts(pow, "aaa")
        assertRejects(pow, "", "a", "aa", "aaaa")

        // Loop
        val loop = fromRegex(mkRegexLoop(1, 2, a), alphabet = listOf('a'))
        assertAccepts(loop, "a", "aa")
        assertRejects(loop, "", "aaa")

        // StringToRegex
        val strRe = fromRegex(mkStringToRegex(mkStringLiteral("ab")), alphabet = listOf('a', 'b'))
        assertAccepts(strRe, "ab")
        assertRejects(strRe, "", "a", "aba")
    }

    @Test
    fun testDotAnyChar() = with(KContext()) {
        val x = lit("x")
        val z = lit("z")
        val dot = mkRegexAllChar()
        val regex = mkRegexConcat(mkRegexConcat(x, dot), z)

        val nfa = fromRegex(regex, alphabet = listOf('x', 'z', 'a', 'b'))

        assertAccepts(nfa, "xaz", "xbz", "xzz")
        assertRejects(nfa, "xz", "xazz", "xcz")
    }

    private fun KContext.lit(value: String): KExpr<KRegexSort> =
        mkStringToRegex(mkStringLiteral(value))

    private fun assertAccepts(nfa: NFA, vararg words: String) {
        for (word in words) {
            assertTrue(nfa.check(word), "Expected to accept '$word'")
        }
    }

    private fun assertRejects(nfa: NFA, vararg words: String) {
        for (word in words) {
            assertFalse(nfa.check(word), "Expected to reject '$word'")
        }
    }
}
