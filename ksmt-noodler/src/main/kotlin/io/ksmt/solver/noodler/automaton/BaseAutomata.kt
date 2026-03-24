package io.ksmt.solver.noodler.automaton

val ALL_CHAR_ALPHABET = CharRange(Char(1), Char(255)).toList()

class BaseAutomata(
    val alphabet: List<Char> = ALL_CHAR_ALPHABET,
) {
    internal fun epsilon(): NFA {
        val base = NFANode(true, mutableMapOf())
        base.put(EPS, base)
        return NFA(base, alphabet)
    }

    internal fun allChar(): NFA {
        val start = NFANode(false, mutableMapOf())
        val finish = NFANode(true, mutableMapOf())
        for (letter in alphabet) {
            start.put(letter, finish)
        }
        return NFA(start, alphabet)
    }

    internal fun allWord(): NFA {
        val base = NFANode(true, mutableMapOf())
        for (letter in alphabet) {
            base.put(letter, base)
        }
        return NFA(base, alphabet)
    }

    internal fun word(word: String): NFA {
        val start = NFANode(false, mutableMapOf())
        var current = start
        for (letter in word) {
            val next = NFANode(false, mutableMapOf())
            current.put(letter, next)
            current = next
        }
        current.terminal = true
        return NFA(start, alphabet)
    }

    internal fun range(from: Char, to: Char): NFA {
        val start = NFANode(false, mutableMapOf())
        val finish = NFANode(true, mutableMapOf())
        for (letter in CharRange(from, to)) {
            start.put(letter, finish)
        }
        return NFA(start, alphabet)
    }

    internal fun empty(): NFA {
        val start = NFANode(false, mutableMapOf())
        return NFA(start, alphabet)
    }
}
