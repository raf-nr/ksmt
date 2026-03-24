package io.ksmt.solver.noodler.automaton

class DFA(start: DFANode, alphabet: List<Char>) :
    Automaton<DFANode, DFA>(start, alphabet) {

    override fun newAutomaton(
        start: DFANode,
        alphabet: List<Char>
    ): DFA {
        return DFA(start, alphabet)
    }

    override fun scanExtraSymbols(visited: MutableSet<DFANode>) {
        // Really does nothing
    }

    fun complement(): DFA {
        val nodes = listNodes(start, { true })
        val newNodes = HashMap<DFANode, DFANode>(nodes.size)
        for (old in nodes) {
            newNodes[old] = DFANode(!old.terminal, mutableMapOf())
        }

        val sink = DFANode(true, mutableMapOf())
        for (letter in alphabet) {
            sink.put(letter, sink)
        }

        for (old in nodes) {
            val current = newNodes[old]!!
            for (letter in alphabet) {
                val next = old.get(letter)
                val mapped = if (next == null) sink else newNodes[next]!!
                current.put(letter, mapped)
            }
        }

        return DFA(newNodes[start]!!, alphabet)
    }

    fun asNFA(): NFA {
        val nodes = listNodes(start, { true })
        val newNodes = HashMap<DFANode, NFANode>(nodes.size)
        for (old in nodes) {
            newNodes[old] = NFANode(old.terminal, mutableMapOf())
        }
        for (old in nodes) {
            val current = newNodes[old]!!
            for ((letter, next) in old.getAll()) {
                current.put(letter, newNodes[next]!!)
            }
        }
        return NFA(newNodes[start]!!, alphabet)
    }

    fun minimize(): DFA {
        val q = ArrayDeque<Pair<MutableSet<DFANode>, Char>>()

        val nonTerminalSet = listNodes(start, { !it.terminal }).toMutableSet()
        val terminalSet = terminal.toMutableSet()
        val currentClasses = mutableSetOf(terminalSet, nonTerminalSet)

        fun addSplitters(splitters: List<MutableSet<DFANode>>) {
            for (c in alphabet) {
                q.addAll(
                    splitters.map {
                        Pair(it, c)
                    }
                )
            }
        }

        addSplitters(listOf(terminalSet, nonTerminalSet))

        while (q.isNotEmpty()) {
            val (splitter, c) = q.removeFirst()
            val toRemove = HashSet<MutableSet<DFANode>>()
            val toAdd = HashSet<MutableSet<DFANode>>()

            for (cls in currentClasses) {
                val withTransitionToSplitter = HashSet<DFANode>()
                val withoutTransitionToSplitter = HashSet<DFANode>()

                for (state in cls) {
                    if (splitter.contains(state.get(c))) {
                        withTransitionToSplitter.add(state)
                    } else {
                        withoutTransitionToSplitter.add(state)
                    }
                }

                if (
                    withoutTransitionToSplitter.isNotEmpty() &&
                    withTransitionToSplitter.isNotEmpty()
                ) {
                    toRemove.add(cls)
                    toAdd.addAll(
                        listOf(
                            withTransitionToSplitter,
                            withoutTransitionToSplitter
                        )
                    )

                    addSplitters(
                        listOf(withTransitionToSplitter,
                        withoutTransitionToSplitter)
                    )
                }
            }

            currentClasses.removeAll(toRemove)
            currentClasses.addAll(toAdd)
        }

        if (currentClasses.all { it.size == 1 } ) {
            // Already minimized
            return this
        }

        return constructDFAFromStateSets(currentClasses.toList())
    }

    private fun constructDFAFromStateSets(stateSets: List<MutableSet<DFANode>>): DFA {
        val setsToNewNodes = HashMap<MutableSet<DFANode>, DFANode>()
        val oldNodesToSets = HashMap<DFANode, MutableSet<DFANode>>()

        val oldNodes = listNodes(start, { true })

        for (stateSet in stateSets) {
            for (state in stateSet) {
                oldNodesToSets[state] = stateSet
            }

            setsToNewNodes[stateSet] = DFANode(
                stateSet.any { it.terminal },
                mutableMapOf()
            )
        }

        for (state in oldNodes) {
            val currentNewState = setsToNewNodes[oldNodesToSets[state]]!!
            for ((c, next) in state.getAll()) {
                val nextNewState = setsToNewNodes[oldNodesToSets[next]]!!
                currentNewState.put(c, nextNewState)
            }
        }

        val startSet = stateSets.find { it.contains(start) }
        val newStartNode = setsToNewNodes[startSet]!!

        return DFA(newStartNode, alphabet)
    }
}
