package io.ksmt.solver.noodler.core

import io.ksmt.expr.KExpr
import io.ksmt.solver.KSolver
import io.ksmt.solver.KSolverConfiguration
import io.ksmt.solver.KSolverStatus
import io.ksmt.sort.KBoolSort
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class SolverInteractor(
    statements: List<KExpr<*>>,
    val alphabet: List<Char>,
    val solver: KSolver<out KSolverConfiguration>
) {
    private val noodler = Noodler(statements, alphabet)

    fun solve(maxRounds: Int = 1, solverTimeout: Duration = 1.minutes): KSolverStatus {
        for (roundCount in 1..maxRounds) {
            // Из-за того, что при запуске решателя с таймаутом я не смог
            // вытащить дополнительные ограничения, которые он генерирует,
            // пока что цикл по раундам бесполезный
            System.err.println("Executing round $roundCount")

            val result = oneRound(solverTimeout)
            if (result != KSolverStatus.UNKNOWN) {
                System.err.println("Found result: $result")
                return result
            }
        }
        System.err.println("Cannot find definite result")
        return KSolverStatus.UNKNOWN
    }

    private fun oneRound(solverTimeout: Duration): KSolverStatus {
        val (result, extraConstraints) = launchNoodler()

        if (!result) {
            return KSolverStatus.UNSAT
        }
        solver.assert(extraConstraints)

        return launchExternalSolver(solverTimeout)
    }

    private fun launchExternalSolver(solverTimeout: Duration): KSolverStatus {
        return solver.check(timeout = solverTimeout)
    }

    private fun launchNoodler(): Pair<Boolean, List<KExpr<KBoolSort>>> {
        return noodler.solve()
    }
}
