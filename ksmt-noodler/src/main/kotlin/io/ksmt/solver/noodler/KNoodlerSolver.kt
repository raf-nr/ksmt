package io.ksmt.solver.noodler

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.solver.KModel
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.solver.KSolverUnsupportedFeatureException
import io.ksmt.solver.noodler.automaton.ALL_CHAR_ALPHABET
import io.ksmt.solver.noodler.core.Noodler
import io.ksmt.solver.noodler.core.toNoodlerStatementOrNull
import io.ksmt.sort.KBoolSort
import kotlin.time.Duration

class KNoodlerSolver(
    private val ctx: KContext,
    private val baseSolver: KSolver<out KSolverConfiguration>,
    private val alphabet: List<Char> = ALL_CHAR_ALPHABET
) : KSolver<KSolverConfiguration> {
    private val assertions = arrayListOf<KExpr<KBoolSort>>()
    private val assertionSet = linkedSetOf<KExpr<KBoolSort>>()
    private val scopeMarks = ArrayDeque<Int>()

    private var lastStatus = KSolverStatus.UNKNOWN
    private var lastReason: String? = null

    @Suppress("UNCHECKED_CAST")
    override fun configure(configurator: KSolverConfiguration.() -> Unit) {
        (baseSolver as KSolver<KSolverConfiguration>).configure(configurator)
    }

    override fun assert(expr: KExpr<KBoolSort>) {
        ctx.ensureContextMatch(expr)

        if (!assertionSet.add(expr)) {
            return
        }

        assertions += expr
        baseSolver.assert(expr)
    }

    override fun assertAndTrack(expr: KExpr<KBoolSort>) = assert(expr)

    override fun push() {
        baseSolver.push()
        scopeMarks.addLast(assertions.size)
    }

    override fun pop(n: UInt) {
        require(n.toInt() <= scopeMarks.size) {
            "Can not pop $n scope levels because current scope level is ${scopeMarks.size}"
        }

        repeat(n.toInt()) {
            baseSolver.pop()

            val scopeMark = scopeMarks.removeLast()
            while (assertions.size > scopeMark) {
                val removed = assertions.removeAt(assertions.lastIndex)
                assertionSet.remove(removed)
            }
        }
    }

    override fun check(timeout: Duration): KSolverStatus {
        lastStatus = KSolverStatus.UNKNOWN
        lastReason = null

        while (true) {
            val noodlerAssertions = assertions.mapNotNull { it.toNoodlerStatementOrNull() }
            if (noodlerAssertions.isEmpty()) {
                lastStatus = baseSolver.check(timeout)
                if (lastStatus == KSolverStatus.UNKNOWN) {
                    lastReason = baseSolver.reasonOfUnknown()
                }
                return lastStatus
            }

            val (isSat, extraConstraints) = Noodler(noodlerAssertions, alphabet).solve()
            if (!isSat) {
                lastStatus = KSolverStatus.UNSAT
                return lastStatus
            }

            val hasFreshConstraints = assertFreshConstraints(extraConstraints)

            val baseStatus = baseSolver.check(timeout)
            if (baseStatus != KSolverStatus.SAT) {
                lastStatus = baseStatus
                if (baseStatus == KSolverStatus.UNKNOWN) {
                    lastReason = baseSolver.reasonOfUnknown()
                }
                return lastStatus
            }

            if (!hasFreshConstraints) {
                lastStatus = KSolverStatus.SAT
                return lastStatus
            }
        }
    }

    override fun checkWithAssumptions(
        assumptions: List<KExpr<KBoolSort>>,
        timeout: Duration
    ): KSolverStatus {
        ctx.ensureContextMatch(assumptions)

        push()
        try {
            assumptions.forEach(::assert)
            return check(timeout)
        } finally {
            pop()
        }
    }

    override fun model(): KModel {
        require(lastStatus == KSolverStatus.SAT) {
            "Model is only available after SAT checks, current solver status: $lastStatus"
        }

        return baseSolver.model()
    }

    override fun unsatCore(): List<KExpr<KBoolSort>> {
        throw KSolverUnsupportedFeatureException("Unsat core is not supported by KNoodlerSolver")
    }

    override fun reasonOfUnknown(): String = lastReason ?: "unknown"

    override fun interrupt() = baseSolver.interrupt()

    override fun close() = baseSolver.close()

    private fun assertFreshConstraints(constraints: List<KExpr<KBoolSort>>): Boolean {
        var hasFreshConstraints = false

        for (constraint in constraints) {
            if (!assertionSet.add(constraint)) {
                continue
            }

            assertions += constraint
            baseSolver.assert(constraint)
            hasFreshConstraints = true
        }

        return hasFreshConstraints
    }
}
