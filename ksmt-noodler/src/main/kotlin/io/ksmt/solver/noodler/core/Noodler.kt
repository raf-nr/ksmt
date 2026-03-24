@file:Suppress("UNCHECKED_CAST")

package io.ksmt.solver.noodler.core

import io.ksmt.expr.KConst
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KStringConcatExpr
import io.ksmt.expr.KStringInRegexExpr
import io.ksmt.solver.noodler.automaton.ALL_CHAR_ALPHABET
import io.ksmt.solver.noodler.automaton.BaseAutomata
import io.ksmt.solver.noodler.automaton.EPS
import io.ksmt.solver.noodler.automaton.NFA
import io.ksmt.solver.noodler.automaton.NFANode
import io.ksmt.solver.noodler.automaton.fromRegex
import io.ksmt.solver.noodler.automaton.toRegex
import io.ksmt.solver.noodler.automaton.transformToDFA
import io.ksmt.sort.KStringSort

class Noodler(
    statements: List<KExpr<*>>,
    val alphabet: List<Char>
) {
    val equations: List<KEqExpr<*>>
    val regularConstraints: List<KStringInRegexExpr>
    val regularConstraintsByVariable: Map<String, NFA>

    init {
        val collectedEquations = mutableListOf<KEqExpr<*>>()
        val collectedRegularConstraints = mutableListOf<KStringInRegexExpr>()
        val collectedRegularConstraintsByVariable = linkedMapOf<String, NFA>()

        statements.forEachIndexed { index, statement ->
            when {
                statement is KStringInRegexExpr -> {
                    collectedRegularConstraints.add(statement)

                    val variableName = statement.extractVariableName(index)
                    val automaton = fromRegex(statement.arg1, alphabet)
                    val a2 = transformToDFA(automaton).minimize()
                    println(a2.alphabet)

                    collectedRegularConstraintsByVariable.compute(variableName) { _, oldAutomaton ->
                        oldAutomaton?.product(automaton) ?: automaton
                    }
                }

                statement is KEqExpr<*> &&
                        statement.lhs.sort is KStringSort &&
                        statement.rhs.sort is KStringSort -> {
                    val lhs = statement.lhs as KExpr<KStringSort>
                    val rhs = statement.rhs as KExpr<KStringSort>
                    require(lhs.isConcatOfStringVariables()) {
                        "Equation at index $index has unsupported left side. " +
                                "Only concatenations of string variables are allowed."
                    }
                    require(rhs.isConcatOfStringVariables()) {
                        "Equation at index $index has unsupported right side. " +
                                "Only concatenations of string variables are allowed."
                    }

                    collectedEquations.add(statement)
                }

                else -> throw IllegalArgumentException(
                    "Unsupported statement at index $index: " +
                            "${statement::class.simpleName}. " +
                            "Only string equations and regular constraints are allowed."
                )
            }
        }

        equations = collectedEquations
        regularConstraints = collectedRegularConstraints
        regularConstraintsByVariable = collectedRegularConstraintsByVariable
    }

    fun solve(): Pair<Boolean, List<KStringInRegexExpr>> {
        val baseAutomata = BaseAutomata(alphabet)
        val refinedByVariable = linkedMapOf<String, NFA>()
        val variablesByName = linkedMapOf<String, KExpr<KStringSort>>()

        fun rememberVariable(variable: KConst<*>) {
            if (variable.sort is KStringSort) {
                variablesByName.putIfAbsent(variable.decl.name, variable as KExpr<KStringSort>)
            }
        }

        equations.forEach { equation ->
            flattenConcatVariables(equation.lhs as KExpr<KStringSort>).forEach(::rememberVariable)
            flattenConcatVariables(equation.rhs as KExpr<KStringSort>).forEach(::rememberVariable)
        }
        regularConstraints.forEach { constraint ->
            val variable = constraint.arg0 as? KConst<*> ?: return@forEach
            rememberVariable(variable)
        }

        fun areEquivalent(lhs: NFA, rhs: NFA): Boolean {
            return lhs.difference(rhs).isEmpty() && rhs.difference(lhs).isEmpty()
        }

        if (regularConstraintsByVariable.values.any { it.isEmpty() }) {
            return false to emptyList()
        }

        for (equation in equations) {
            val (ok, refined) = processEquation(equation)
            if (!ok) {
                return false to emptyList()
            }

            for ((variableName, automaton) in refined) {
                if (automaton.isEmpty()) {
                    return false to emptyList()
                }
                refinedByVariable.compute(variableName) { _, old ->
                    old?.product(automaton) ?: automaton
                }
            }
        }

        val additionalConstraints = mutableListOf<KStringInRegexExpr>()
        for ((variableName, automaton) in refinedByVariable) {
            val baseConstraint = regularConstraintsByVariable[variableName] ?: baseAutomata.allWord()
            val strengthened = baseConstraint.product(automaton)
            if (strengthened.isEmpty()) {
                return false to emptyList()
            }
            if (areEquivalent(baseConstraint, strengthened)) {
                continue
            }

            val variable = variablesByName[variableName] ?: continue
            val regex = strengthened.toRegex(variable.ctx)
            additionalConstraints.add(variable.ctx.mkStringInRegexNoSimplify(variable, regex))
        }

        return true to additionalConstraints
    }

    private fun processEquation(equation: KEqExpr<*>): Pair<Boolean, Map<String, NFA>> {
        fun transformConcatenationToNFASequence(expr: KExpr<*>): List<NFA> {
            return flattenConcatVariables(
                expr as KExpr<KStringSort>
            ).map {
                regularConstraintsByVariable[it.decl.name] ?: BaseAutomata().allWord()
            }
        }

        val rhsVariables = flattenConcatVariables(equation.rhs as KExpr<KStringSort>)
        val leftNFASequence = transformConcatenationToNFASequence(equation.lhs)
        val rightNFASequence = transformConcatenationToNFASequence(equation.rhs)

        val leftAutomaton = transformToDFA(leftNFASequence.drop(1).fold(leftNFASequence.first()) { acc, nfa ->
            acc.concat(nfa)
        }).minimize().asNFA()

        val rightAutomaton = rightNFASequence.drop(1).fold(rightNFASequence.first()) { acc, nfa ->
            acc.concat(nfa)
        }

        leftAutomaton.picture("left.png")
        rightAutomaton.picture("right.png")

        val grid = leftAutomaton.product(rightAutomaton)

        if (grid.isEmpty()) {
            return false to emptyMap()
        }

        val noodles = makeNoodles(grid)
        val result = mutableMapOf<String, NFA>()
        for (noodle in noodles) {
            val resultInNoodle = mutableMapOf<String, NFA>()

            val segments = noodle.map {
                makeSegmentNFA(it)
            }
            val segmentCount = minOf(rhsVariables.size, segments.size)
            for (i in 0 until segmentCount) {
                val variableName = rhsVariables[i].decl.name
                val automaton = segments[i]

                resultInNoodle.compute(variableName) { _, old ->
                    old?.product(automaton) ?: automaton
                }
            }

            for ((variableName, automaton) in resultInNoodle) {
                result.compute(variableName) { _, old ->
                    old?.union(automaton) ?: automaton
                }
            }
        }

        return true to result
    }

    private fun makeNoodles(grid: NFA): List<List<List<NFANode>>> {
        val allNoodles = mutableListOf<List<List<NFANode>>>()

        fun dfs(
            v: NFANode,
            currentSegment: MutableList<NFANode>,
            currentSegmentSet: MutableSet<NFANode>,
            currentNoodle: MutableList<List<NFANode>>,
            stagePathNodes: MutableSet<NFANode>
        ) {
            currentSegment.add(v)
            currentSegmentSet.add(v)

            for ((letter, nextNodes) in v.getAllList()) {
                if (letter == EPS) {
                    continue
                }

                for (next in nextNodes) {
                    if (next !in currentSegmentSet) {
                        dfs(next, currentSegment, currentSegmentSet, currentNoodle, stagePathNodes)
                    }
                }
            }

            val nextStageNodes = v.getList(EPS)
            currentNoodle.add(currentSegment.toList())

            if (nextStageNodes.isEmpty()) {
                if (currentSegment.any { it.terminal }) {
                    allNoodles.add(currentNoodle.map { it.toList() })
                }
            } else {
                for (nextStageNode in nextStageNodes) {
                    if (!stagePathNodes.add(nextStageNode)) {
                        continue
                    }
                    dfs(
                        v = nextStageNode,
                        currentSegment = mutableListOf(),
                        currentSegmentSet = mutableSetOf(),
                        currentNoodle = currentNoodle,
                        stagePathNodes = stagePathNodes
                    )
                    stagePathNodes.remove(nextStageNode)
                }
            }

            currentNoodle.removeLast()
            currentSegment.removeLast()
            currentSegmentSet.remove(v)
        }

        dfs(
            v = grid.start,
            currentSegment = mutableListOf(),
            currentSegmentSet = mutableSetOf(),
            currentNoodle = mutableListOf(),
            stagePathNodes = mutableSetOf(grid.start)
        )
        return allNoodles
    }

    private fun makeSegmentNFA(nodes: List<NFANode>): NFA {
        val nodeSet = nodes.toSet()

        for (node in nodes) {
            val edges = node.getAllList().map { (letter, nextNodes) ->
                letter to nextNodes.toList()
            }
            for ((letter, nextNodes) in edges) {
                val newNextNodes = nextNodes.filter { nodeSet.contains(it) }
                node.remove(letter)
                newNextNodes.forEach { node.put(letter, it) }
            }
        }

        nodes.last().terminal = true
        return NFA(nodes.first(), ALL_CHAR_ALPHABET)
    }
}

private fun KStringInRegexExpr.extractVariableName(index: Int): String {
    val variable = arg0 as? KConst<*>
    require(variable != null && variable.sort is KStringSort) {
        "Regular constraint at index $index must use a string variable on the left-hand side"
    }
    return variable.decl.name
}

private fun flattenConcatVariables(expr: KExpr<*>): List<KConst<*>> {
    return when (expr) {
        is KStringConcatExpr -> flattenConcatVariables(expr.arg0) + flattenConcatVariables(expr.arg1)
        is KConst<*> -> {
            require(expr.sort is KStringSort) { "Expected string variable in equation side" }
            listOf(expr)
        }

        else -> error("Expected concatenation of string variables")
    }
}
