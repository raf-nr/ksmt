package io.ksmt.expr

import io.ksmt.KContext
import io.ksmt.cache.hash
import io.ksmt.cache.structurallyEqual
import io.ksmt.decl.KDecl
import io.ksmt.decl.KStringLenDecl
import io.ksmt.decl.KStringLiteralDecl
import io.ksmt.decl.KStringLtDecl
import io.ksmt.decl.KStringLeDecl
import io.ksmt.decl.KStringGtDecl
import io.ksmt.decl.KStringGeDecl
import io.ksmt.decl.KStringToRegexDecl
import io.ksmt.decl.KStringInRegexDecl
import io.ksmt.decl.KStringContainsDecl
import io.ksmt.decl.KStringReplaceDecl
import io.ksmt.decl.KStringReplaceAllDecl
import io.ksmt.decl.KStringReplaceWithRegexDecl
import io.ksmt.decl.KStringReplaceAllWithRegexDecl
import io.ksmt.decl.KStringIsDigitDecl
import io.ksmt.decl.KStringToCodeDecl
import io.ksmt.decl.KStringFromCodeDecl
import io.ksmt.decl.KStringToIntDecl
import io.ksmt.decl.KStringFromIntDecl
import io.ksmt.expr.transformer.KTransformerBase
import io.ksmt.sort.KSort
import io.ksmt.sort.KBoolSort
import io.ksmt.sort.KIntSort
import io.ksmt.sort.KRegexSort
import io.ksmt.sort.KStringSort
import io.ksmt.utils.cast

class KStringLiteralExpr internal constructor(
    ctx: KContext,
    val value: String
) : KInterpretedValue<KStringSort>(ctx) {
    override val sort: KStringSort
        get() = ctx.stringSort

    override val decl: KStringLiteralDecl
        get() = ctx.mkStringLiteralDecl(value)

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(value)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { value }
}

class KStringConcatExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KStringSort>
) : KApp<KStringSort, KStringSort>(ctx) {
    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg0, arg1)

    override val decl: KDecl<KStringSort>
        get() = ctx.mkStringConcatDecl()

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override val sort: KStringSort = ctx.mkStringSort()

    override fun internHashCode(): Int = hash(arg0, arg1)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 })
}

class KStringLenExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KStringSort>
) : KApp<KIntSort, KStringSort>(ctx) {
    override val sort: KIntSort = ctx.intSort

    override val decl: KStringLenDecl
        get() = ctx.mkStringLenDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KIntSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}

class KStringToRegexExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KStringSort>
) : KApp<KRegexSort, KStringSort>(ctx) {
    override val sort: KRegexSort = ctx.regexSort

    override val decl: KStringToRegexDecl
        get() = ctx.mkStringToRegexDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KRegexSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}

class KStringInRegexExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KRegexSort>
) : KApp<KBoolSort, KSort>(ctx) {
    override val sort: KBoolSort = ctx.mkBoolSort()

    override val args: List<KExpr<KSort>>
        get() = listOf(arg0.cast(), arg1.cast())

    override val decl: KStringInRegexDecl
        get() = ctx.mkStringInRegexDecl()

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 })
}

class KSuffixOfExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.mkBoolSort()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg0, arg1)

    override val decl: KDecl<KBoolSort>
        get() = ctx.mkSuffixOfDecl()

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 })
}

class KPrefixOfExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.mkBoolSort()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg0, arg1)

    override val decl: KDecl<KBoolSort>
        get() = ctx.mkPrefixOfDecl()

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 })
}

class KStringLtExpr internal constructor(
    ctx: KContext,
    val lhs: KExpr<KStringSort>,
    val rhs: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.boolSort

    override val decl: KStringLtDecl
        get() = ctx.mkStringLtDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(lhs, rhs)

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(lhs, rhs)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { lhs }, { rhs })
}

class KStringLeExpr internal constructor(
    ctx: KContext,
    val lhs: KExpr<KStringSort>,
    val rhs: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.boolSort

    override val decl: KStringLeDecl
        get() = ctx.mkStringLeDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(lhs, rhs)

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(lhs, rhs)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { lhs }, { rhs })
}

class KStringGtExpr internal constructor(
    ctx: KContext,
    val lhs: KExpr<KStringSort>,
    val rhs: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.boolSort

    override val decl: KStringGtDecl
        get() = ctx.mkStringGtDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(lhs, rhs)

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(lhs, rhs)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { lhs }, { rhs })
}

class KStringGeExpr internal constructor(
    ctx: KContext,
    val lhs: KExpr<KStringSort>,
    val rhs: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.boolSort

    override val decl: KStringGeDecl
        get() = ctx.mkStringGeDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(lhs, rhs)

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(lhs, rhs)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { lhs }, { rhs })
}

class KStringContainsExpr internal constructor(
    ctx: KContext,
    val lhs: KExpr<KStringSort>,
    val rhs: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.boolSort

    override val decl: KStringContainsDecl
        get() = ctx.mkStringContainsDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(lhs, rhs)

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(lhs, rhs)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { lhs }, { rhs })
}

class KSingletonSubstringExpr : RuntimeException("Not yet implemented")

class KSubstringExpr : RuntimeException("Not yet implemented")

class KIndexOfExpr : RuntimeException("Not yet implemented")

class KStringReplaceExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KStringSort>,
    val arg2: KExpr<KStringSort>
) : KApp<KStringSort, KStringSort>(ctx) {
    override val sort: KStringSort = ctx.stringSort

    override val decl: KStringReplaceDecl
        get() = ctx.mkStringReplaceDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg0, arg1, arg2)

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1, arg2)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 }, { arg2 })
}

class KStringReplaceAllExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KStringSort>,
    val arg2: KExpr<KStringSort>
) : KApp<KStringSort, KStringSort>(ctx) {
    override val sort: KStringSort = ctx.stringSort

    override val decl: KStringReplaceAllDecl
        get() = ctx.mkStringReplaceAllDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg0, arg1, arg2)

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1, arg2)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 }, { arg2 })
}

class KStringReplaceWithRegexExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KRegexSort>,
    val arg2: KExpr<KStringSort>
) : KApp<KStringSort, KSort>(ctx) {
    override val sort: KStringSort = ctx.stringSort

    override val decl: KStringReplaceWithRegexDecl
        get() = ctx.mkStringReplaceWithRegexDecl()

    override val args: List<KExpr<KSort>>
        get() = listOf(arg0.cast(), arg1.cast(), arg2.cast())

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1, arg2)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 }, { arg2 })
}

class KStringReplaceAllWithRegexExpr internal constructor(
    ctx: KContext,
    val arg0: KExpr<KStringSort>,
    val arg1: KExpr<KRegexSort>,
    val arg2: KExpr<KStringSort>
) : KApp<KStringSort, KSort>(ctx) {
    override val sort: KStringSort = ctx.stringSort

    override val decl: KStringReplaceAllWithRegexDecl
        get() = ctx.mkStringReplaceAllWithRegexDecl()

    override val args: List<KExpr<KSort>>
        get() = listOf(arg0.cast(), arg1.cast(), arg2.cast())

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg0, arg1, arg2)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other, { arg0 }, { arg1 }, { arg2 })
}

/*
    Maps to and from integers.
 */

class KStringIsDigitExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KStringSort>
) : KApp<KBoolSort, KStringSort>(ctx) {
    override val sort: KBoolSort = ctx.boolSort

    override val decl: KStringIsDigitDecl
        get() = ctx.mkStringIsDigitDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KBoolSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}

class KStringToCodeExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KStringSort>
) : KApp<KIntSort, KStringSort>(ctx) {
    override val sort: KIntSort = ctx.intSort

    override val decl: KStringToCodeDecl
        get() = ctx.mkStringToCodeDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KIntSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}

class KStringFromCodeExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KIntSort>
) : KApp<KStringSort, KIntSort>(ctx) {
    override val sort: KStringSort = ctx.stringSort

    override val decl: KStringFromCodeDecl
        get() = ctx.mkStringFromCodeDecl()

    override val args: List<KExpr<KIntSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}

class KStringToIntExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KStringSort>
) : KApp<KIntSort, KStringSort>(ctx) {
    override val sort: KIntSort = ctx.intSort

    override val decl: KStringToIntDecl
        get() = ctx.mkStringToIntDecl()

    override val args: List<KExpr<KStringSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KIntSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}

class KStringFromIntExpr internal constructor(
    ctx: KContext,
    val arg: KExpr<KIntSort>
) : KApp<KStringSort, KIntSort>(ctx) {
    override val sort: KStringSort = ctx.stringSort

    override val decl: KStringFromIntDecl
        get() = ctx.mkStringFromIntDecl()

    override val args: List<KExpr<KIntSort>>
        get() = listOf(arg)

    override fun accept(transformer: KTransformerBase): KExpr<KStringSort> {
        TODO("Not yet implemented")
    }

    override fun internHashCode(): Int = hash(arg)
    override fun internEquals(other: Any): Boolean = structurallyEqual(other) { arg }
}
