package io.ksmt.solver.noodler.core

import io.ksmt.expr.KConst
import io.ksmt.expr.KEqExpr
import io.ksmt.expr.KExpr
import io.ksmt.expr.KStringConcatExpr
import io.ksmt.expr.KStringInRegexExpr
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KStringSort

internal fun KExpr<KBoolSort>.toNoodlerStatementOrNull(): KExpr<*>? = when (this) {
    is KStringInRegexExpr -> {
        val variable = arg0 as? KConst<*>
        if (variable != null && variable.sort is KStringSort) {
            this
        } else {
            null
        }
    }

    is KEqExpr<*> -> {
        if (lhs.sort is KStringSort && rhs.sort is KStringSort &&
            lhs.isConcatOfStringVariables() && rhs.isConcatOfStringVariables()
        ) {
            this
        } else {
            null
        }
    }

    else -> null
}

internal fun KExpr<*>.isConcatOfStringVariables(): Boolean = when (this) {
    is KStringConcatExpr -> arg0.isConcatOfStringVariables() && arg1.isConcatOfStringVariables()
    is KConst<*> -> sort is KStringSort
    else -> false
}
