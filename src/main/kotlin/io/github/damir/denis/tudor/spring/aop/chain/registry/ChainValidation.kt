package io.github.damir.denis.tudor.spring.aop.chain.registry

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.github.damir.denis.tudor.spring.aop.chain.registry")

internal fun Map<String, ChainNode>.validateChainTypes() {
    values
        .filter { it.nextId != null }
        .forEach { node ->
            val next = this[node.nextId]
                ?: error("Chain '${node.id}' references unknown next '${node.nextId}'")

            if (!next.inputType.isAssignableFrom(node.outputType)) {
                error(
                    "Type mismatch: '${node.id}' outputs ${node.outputType.simpleName} " +
                            "but '${next.id}' expects ${next.inputType.simpleName}"
                )
            }
        }
}

internal fun Map<String, ChainNode>.detectChainCycles() {
    val visited = mutableSetOf<String>()
    val visiting = mutableSetOf<String>()

    fun dfs(nodeId: String): Boolean {
        if (nodeId in visiting) return true
        if (!visited.add(nodeId)) return false
        visiting += nodeId
        this[nodeId]?.nextId?.let { if (dfs(it)) return true }
        visiting -= nodeId
        return false
    }

    keys.forEach {
        if (dfs(it)) error("Cycle detected in chain starting from: $it")
    }
}

internal fun Map<String, ChainNode>.logChains() {
    val referenced = values.mapNotNull { it.nextId }.toSet()
    val startNodes = values.filter { it.id !in referenced }

    startNodes.forEachIndexed { index, start ->
        val chain = generateSequence(start) { node ->
            node.nextId?.let { this[it] }
        }.joinToString("\n   -> ") { "${it.id}: ${it.inputType} -> ${it.outputType}" }

        logger.info("Chain {}: \n{}", index + 1, chain)
    }
}
