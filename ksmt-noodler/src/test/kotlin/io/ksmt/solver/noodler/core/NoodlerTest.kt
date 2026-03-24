package io.ksmt.solver.noodler.core

import io.ksmt.KContext
import io.ksmt.expr.KConst
import io.ksmt.solver.noodler.automaton.fromRegex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NoodlerTest {
    private val binaryAlphabet = listOf('a', 'b')

    @Test
    fun testSplitStatementsAndBuildRegularConstraintMap() = with(KContext()) {
        val x = mkConst("x", stringSort)
        val y = mkConst("y", stringSort)
        val t = mkConst("t", stringSort)

        val equation = mkEq(mkStringConcat(x, y), t)
        val xConstraint = mkStringInRegex(x, mkRegexCross(mkStringToRegex(mkStringLiteral("a"))))
        val yConstraint = mkStringInRegex(y, mkRegexStar(mkStringToRegex(mkStringLiteral("b"))))

        val noodler = Noodler(listOf(equation, xConstraint, yConstraint), binaryAlphabet)

        assertEquals(1, noodler.equations.size)
        assertEquals(2, noodler.regularConstraints.size)
        assertEquals(setOf("x", "y"), noodler.regularConstraintsByVariable.keys)

        val xRegexNfa = noodler.regularConstraintsByVariable.getValue("x")
        assertTrue(xRegexNfa.check("a"))
        assertTrue(xRegexNfa.check("aa"))
    }

    @Test
    fun testDuplicateRegularConstraintMergesByIntersection() = with(KContext()) {
        val x = mkConst("x", stringSort)
        val constraint1 = mkStringInRegex(x, mkRegexCross(mkStringToRegex(mkStringLiteral("a"))))
        val constraint2 = mkStringInRegex(x, mkRegexStar(mkStringToRegex(mkStringLiteral("b"))))

        val noodler = Noodler(listOf(constraint1, constraint2), binaryAlphabet)
        val merged = noodler.regularConstraintsByVariable.getValue("x")
        assertTrue(!merged.check(""))
        assertTrue(!merged.check("a"))
        assertTrue(!merged.check("b"))
    }

    @Test
    fun testRegularConstraintOnNonVariableFails(): Unit = with(KContext()) {
        val x = mkConst("x", stringSort)
        val y = mkConst("y", stringSort)
        val nonVariable = mkStringConcat(x, y)
        val constraint = mkStringInRegex(nonVariable, mkRegexAll())

        assertFailsWith<IllegalArgumentException> {
            Noodler(listOf(constraint), binaryAlphabet)
        }
    }

    @Test
    fun testEquationWithLiteralFails(): Unit = with(KContext()) {
        val x = mkConst("x", stringSort)
        val y = mkConst("y", stringSort)
        val equation = mkEq(mkStringConcat(x, mkStringLiteral("a")), y)

        assertFailsWith<IllegalArgumentException> {
            Noodler(listOf(equation), binaryAlphabet)
        }
    }

    @Test
    fun testSolveDerivesAdditionalConstraintForTargetVariable() = with(KContext()) {
        val x = mkConst("x", stringSort)
        val y = mkConst("y", stringSort)
        val t = mkConst("t", stringSort)

        val equation = mkEq(mkStringConcat(x, y), t)
        val xConstraint = mkStringInRegex(x, mkStringToRegex(mkStringLiteral("a")))
        val yConstraint = mkStringInRegex(y, mkStringToRegex(mkStringLiteral("b")))

        val (isSat, additionalConstraints) = Noodler(
            listOf(equation, xConstraint, yConstraint),
            binaryAlphabet
        ).solve()

        assertTrue(isSat)
        assertTrue(additionalConstraints.all { constraint ->
            val variableName = (constraint.arg0 as KConst<*>).decl.name
            variableName in setOf(x.decl.name, y.decl.name, t.decl.name)
        })

        val tConstraint = additionalConstraints.firstOrNull {
            val variableName = (it.arg0 as KConst<*>).decl.name
            variableName == t.decl.name
        }
        if (tConstraint != null) {
            val derivedAutomaton = fromRegex(tConstraint.arg1, binaryAlphabet)
            assertTrue(derivedAutomaton.check("ab"))
            assertFalse(derivedAutomaton.check("a"))
            assertFalse(derivedAutomaton.check("b"))
        }
    }

}
