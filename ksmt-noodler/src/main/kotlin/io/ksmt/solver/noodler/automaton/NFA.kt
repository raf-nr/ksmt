package io.ksmt.solver.noodler.automaton


val EPS = Char(0)


class NFA(start: NFANode, alphabet: List<Char>) :
    Automaton<NFANode, NFA>(start, alphabet) {

    fun concat(other: NFA): NFA {
        val thisCopy = copy() as NFA
        val otherCopy = other.copy() as NFA
        for (state in thisCopy.terminal) {
            state.put(EPS, otherCopy.start)
        }
        thisCopy.alphabet = thisCopy.alphabet.toSet().union(otherCopy.alphabet.toSet()).toList()
        val allNodes = listNodes(thisCopy.start, { true })
        for (node in allNodes) {
            node.terminal = false
        }
        for (node in otherCopy.terminal) {
            node.terminal = true
        }
        thisCopy.terminal = otherCopy.terminal
        return thisCopy
    }

    fun product(other: NFA): NFA {
        val newAlphabet = alphabet.toSet().union(other.alphabet).toList()

        val q = ArrayDeque<Triple<NFANode, NFANode, NFANode>>()

        val productStart = product(start, other.start)
        val pairsToProduct = mutableMapOf<Pair<NFANode, NFANode>, NFANode>()
        pairsToProduct[Pair(start, other.start)] = productStart

        q.add(Triple(start, other.start, productStart))

        while (q.isNotEmpty()) {
            val (currentThis, currentOther, currentProduct) = q.removeFirst()

            fun processProductNode(one: NFANode, other: NFANode): NFANode {
                val pair = Pair(one, other)
                val existing = pairsToProduct[pair]
                if (existing != null) {
                    return existing
                }
                val newProductNode = product(one, other)
                pairsToProduct[pair] = newProductNode
                q.add(Triple(one, other, newProductNode))
                return newProductNode
            }

            fun addEdge(letter: Char, one: NFANode, other: NFANode) {
                val next = processProductNode(one, other)
                currentProduct.put(letter, next)
            }

            for (ths in currentThis.getList(EPS)) {
                addEdge(EPS, ths, currentOther)
            }
            for (oth in currentOther.getList(EPS)) {
                addEdge(EPS, currentThis, oth)
            }

            for (letter in currentProduct.getAllList().keys) {
                if (letter == EPS) {
                    continue
                }
                for (ths in currentThis.getList(letter)) {
                    for (oth in currentOther.getList(letter)) {
                        addEdge(letter, ths, oth)
                    }
                }
            }
        }

        return NFA(productStart, newAlphabet)
    }

    fun union(other: NFA): NFA {
        val newStart = NFANode(start.terminal || other.start.terminal, mutableMapOf())
        val newAlphabet = alphabet.toSet().union(other.alphabet.toSet()).toList()
        newStart.put(EPS, start)
        newStart.put(EPS, other.start)
        return NFA(newStart, newAlphabet)
    }

    fun kleeneClosure(): NFA {
        val result = copy()

        for (node in result.terminal) {
            node.put(EPS, result.start)
        }
        result.start.terminal = true
        result.terminal = listNodes(result.start, { it.terminal })

        return result as NFA
    }

    fun power(a: Int): NFA {
        require(a >= 0) { "power must be non-negative" }
        if (a == 0) {
            val start = NFANode(true, mutableMapOf())
            return NFA(start, alphabet)
        }

        var result = copy() as NFA
        repeat(a - 1) {
            result = result.concat(copy() as NFA)
        }
        return result
    }

    fun difference(other: NFA): NFA {
        val unionAlphabet = alphabet.toSet().union(other.alphabet).toList()

        val left = (copy() as NFA).apply { alphabet = unionAlphabet }
        val right = (other.copy() as NFA).apply { alphabet = unionAlphabet }

        val rightComplement = transformToDFA(right).complement().asNFA()
        return left.product(rightComplement)
    }

    fun loop(from: Int, to: Int): NFA {
        require(from >= 0) { "from must be non-negative" }
        require(to >= from) { "to must be >= from" }

        var currentPower = power(from)
        var result = currentPower.copy() as NFA
        for (i in (from + 1)..to) {
            currentPower = currentPower.concat(copy() as NFA)
            result = result.union(currentPower)
        }
        return result
    }

    override fun newAutomaton(
        start: NFANode,
        alphabet: List<Char>
    ): Automaton<NFANode, NFA> {
        return NFA(start, alphabet)
    }

    override fun scanExtraSymbols(visited: MutableSet<NFANode>) {
        epsilonClosure(visited)
    }
}

fun transformToDFA(a: NFA): DFA {
    val symbols = a.alphabet.filter { it != EPS }
    val startSet = mutableSetOf(a.start)
    epsilonClosure(startSet)
    val newStart = DFANode(startSet.any { it.terminal }, mutableMapOf())

    val q = ArrayDeque<Pair<Set<NFANode>, DFANode>>()
    val visited: MutableMap<Set<NFANode>, DFANode> = mutableMapOf()
    visited[startSet] = newStart
    q.add(Pair(startSet, newStart))

    while (q.isNotEmpty()) {
        val (currentStateSet, currentDFANode) = q.removeFirst()

        for (c in symbols) {
            val newStateSet = LinkedHashSet<NFANode>()
            for (state in currentStateSet) {
                newStateSet.addAll(state.getList(c))
            }
            epsilonClosure(newStateSet)

            val nextDFANode = visited[newStateSet] ?: run {
                val node = DFANode(newStateSet.any { it.terminal }, mutableMapOf())
                visited[newStateSet] = node
                q.add(Pair(newStateSet, node))
                node
            }
            currentDFANode.put(c, nextDFANode)
        }
    }

    return DFA(newStart, symbols)
}

fun epsilonClosure(visited: MutableSet<NFANode>) {
    val queue = ArrayDeque<NFANode>()
    queue.addAll(visited)

    while (queue.isNotEmpty()) {
        val state = queue.removeFirst()
        for (next in state.getList(EPS)) {
            if (visited.add(next)) {
                queue.add(next)
            }
        }
    }
}
