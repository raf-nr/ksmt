package io.ksmt.solver.noodler.core

import io.ksmt.KContext

import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration

fun main() = with(KContext()) {
    val z = mkConst("z", stringSort)
    val y = mkConst("y", stringSort)

    // zy = z
    // y in a+b+
    // z in b+
    val equation = mkEq(mkStringConcat(z, y), z)

    val aPlus = mkRegexCross(mkStringToRegex(mkStringLiteral("a")))
    val bPlus = mkRegexCross(mkStringToRegex(mkStringLiteral("b")))
    val yConstraint = mkStringInRegex(y, mkRegexConcat(aPlus, bPlus))
    val zConstraint = mkStringInRegex(z, bPlus)

    val assertions = listOf(equation, yConstraint, zConstraint)

    val n = Noodler(assertions, listOf('a', 'b'))

    val (sat, extra) = n.solve()
    println(sat)
    for (a in extra) {
        println(a)
    }

    val interactor = SolverInteractor(assertions,
        listOf('a', 'b'), createZ3SolverOrNull(this)!!)

    println("Interactor result: ${interactor.solve()}")
}

private fun createZ3SolverOrNull(ctx: KContext): KSolver<out KSolverConfiguration>? {
    return try {
        val solverClass = Class.forName("io.ksmt.solver.z3.KZ3Solver")
        val ctor = solverClass.getConstructor(KContext::class.java)
        @Suppress("UNCHECKED_CAST")
        ctor.newInstance(ctx) as KSolver<out KSolverConfiguration>
    } catch (_: Throwable) {
        null
    }
}
