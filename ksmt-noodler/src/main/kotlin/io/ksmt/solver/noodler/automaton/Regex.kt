package io.ksmt.solver.noodler.automaton

import io.ksmt.KContext
import io.ksmt.expr.KExpr
import io.ksmt.expr.KRegexAll
import io.ksmt.expr.KRegexAllChar
import io.ksmt.expr.KRegexComplementExpr
import io.ksmt.expr.KRegexConcatExpr
import io.ksmt.expr.KRegexCrossExpr
import io.ksmt.expr.KRegexDifferenceExpr
import io.ksmt.expr.KRegexEpsilon
import io.ksmt.expr.KRegexIntersectionExpr
import io.ksmt.expr.KRegexLoopExpr
import io.ksmt.expr.KRegexOptionExpr
import io.ksmt.expr.KRegexPowerExpr
import io.ksmt.expr.KRegexRangeExpr
import io.ksmt.expr.KRegexStarExpr
import io.ksmt.expr.KRegexUnionExpr
import io.ksmt.expr.KStringLiteralExpr
import io.ksmt.expr.KStringToRegexExpr
import io.ksmt.sort.KRegexSort

fun fromRegex(expr: KExpr<KRegexSort>, alphabet: List<Char> = ALL_CHAR_ALPHABET): NFA {
    val base = BaseAutomata(alphabet)
    return when (expr) {
        is KRegexEpsilon -> base.empty()
        is KRegexAll -> base.allWord()
        is KRegexAllChar -> base.allChar()
        is KRegexConcatExpr -> fromRegex(expr.arg0, alphabet).concat(fromRegex(expr.arg1, alphabet))
        is KRegexUnionExpr -> fromRegex(expr.arg0, alphabet).union(fromRegex(expr.arg1, alphabet))
        is KRegexIntersectionExpr -> fromRegex(expr.arg0, alphabet).product(fromRegex(expr.arg1, alphabet))
        is KRegexStarExpr -> fromRegex(expr.arg, alphabet).kleeneClosure()
        is KRegexCrossExpr -> {
            val a = fromRegex(expr.arg, alphabet)
            a.concat(a.kleeneClosure())
        }
        is KRegexDifferenceExpr -> fromRegex(expr.arg0, alphabet).difference(fromRegex(expr.arg1, alphabet))
        is KRegexComplementExpr -> transformToDFA(fromRegex(expr.arg, alphabet)).complement().asNFA()
        is KRegexOptionExpr -> fromRegex(expr.arg, alphabet).union(base.epsilon())
        is KRegexRangeExpr -> rangeFromLiterals(expr, base)
        is KRegexPowerExpr -> fromRegex(expr.arg, alphabet).power(expr.power)
        is KRegexLoopExpr -> fromRegex(expr.arg, alphabet).loop(expr.from, expr.to)
        is KStringToRegexExpr -> fromString(expr, base)
        else -> unsupported(expr, "unknown expression type")
    }
}

private fun fromString(expr: KStringToRegexExpr, base: BaseAutomata): NFA {
    return when (val arg = expr.arg) {
        is KStringLiteralExpr -> base.word(arg.value)
        else -> unsupported(expr, "not a string literal in regex")
    }
}

private fun rangeFromLiterals(expr: KRegexRangeExpr, base: BaseAutomata): NFA {
    val from = expr.arg0 as? KStringLiteralExpr
    val to = expr.arg1 as? KStringLiteralExpr
    if (from == null || to == null ||
        from.value.length != 1 || to.value.length != 1) {
        unsupported(expr, "range values are invalid")
    }
    return base.range(from.value[0], to.value[0])
}

private fun unsupported(expr: KExpr<*>, reason: String): Nothing =
    error("fromRegex: unsupported expression: ${expr::class.simpleName} because of: $reason")

fun NFA.toRegex(ctx: KContext): KExpr<KRegexSort> {
    val nodes = listNodes(start, { true })
    val n = nodes.size
    val startId = n
    val finalId = n + 1

    val emptyLang = ctx.mkRegexEpsilon()
    val epsilon = ctx.mkStringToRegex(ctx.mkStringLiteral(""))

    val idOf = nodes.withIndex().associate { it.value to it.index }
    val R = HashMap<Int, HashMap<Int, KExpr<KRegexSort>>>()

    fun getR(i: Int, j: Int): KExpr<KRegexSort> =
        R[i]?.get(j) ?: emptyLang

    fun setR(i: Int, j: Int, expr: KExpr<KRegexSort>) {
        if (expr == emptyLang) return
        R.getOrPut(i) { HashMap() }[j] = expr
    }

    fun regexUnion(a: KExpr<KRegexSort>, b: KExpr<KRegexSort>): KExpr<KRegexSort> =
        when {
            a == emptyLang -> b
            b == emptyLang -> a
            else -> ctx.mkRegexUnion(a, b)
        }

    fun regexConcat(a: KExpr<KRegexSort>, b: KExpr<KRegexSort>): KExpr<KRegexSort> =
        when {
            a == emptyLang || b == emptyLang -> emptyLang
            a == epsilon -> b
            b == epsilon -> a
            else -> ctx.mkRegexConcat(a, b)
        }

    fun regexStar(a: KExpr<KRegexSort>): KExpr<KRegexSort> =
        when (a) {
            emptyLang, epsilon -> epsilon
            else -> ctx.mkRegexStar(a)
        }

    for (u in nodes) {
        val i = idOf.getValue(u)
        for ((letter, nexts) in u.getAllList()) {
            val label = if (letter == EPS) {
                epsilon
            } else {
                ctx.mkStringToRegex(ctx.mkStringLiteral(letter.toString()))
            }
            for (v in nexts) {
                val j = idOf.getValue(v)
                val prev = getR(i, j)
                setR(i, j, regexUnion(prev, label))
            }
        }
    }

    // New start -> old start
    setR(startId, idOf.getValue(start), epsilon)
    // Old terminals -> new final
    for (t in terminal) {
        setR(idOf.getValue(t), finalId, epsilon)
    }

    val allIds = (0 until n).toMutableList()
    allIds.add(startId)
    allIds.add(finalId)

    for (k in allIds.toList()) {
        if (k == startId || k == finalId) continue
        val rkk = getR(k, k)
        val rkkStar = regexStar(rkk)

        for (i in allIds) {
            if (i == k) continue
            val rik = getR(i, k)
            if (rik == emptyLang) continue
            for (j in allIds) {
                if (j == k) continue
                val rkj = getR(k, j)
                if (rkj == emptyLang) continue
                val rij = getR(i, j)
                val through = regexConcat(rik, regexConcat(rkkStar, rkj))
                setR(i, j, regexUnion(rij, through))
            }
        }

        R.remove(k)
        for (m in R.values) {
            m.remove(k)
        }
    }

    return getR(startId, finalId)
}
