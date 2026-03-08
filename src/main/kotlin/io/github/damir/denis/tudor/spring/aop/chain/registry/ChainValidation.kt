package io.github.damir.denis.tudor.spring.aop.chain.registry

import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStart
import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.context.ApplicationContext
import java.lang.reflect.ParameterizedType

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

internal fun Map<String, ChainNode>.lastNode(startId: String): ChainNode {
    val node = this[startId] ?: error("No chain node found for: $startId")
    return generateSequence(node) { it.nextId?.let(this::get) }.last()
}

internal fun Map<String, ChainNode>.validateChainStartMethods(ctx: ApplicationContext) {
    ctx.getBeansOfType(Any::class.java).values.forEach { bean ->
        val targetClass = AopUtils.getTargetClass(bean)
        targetClass.methods
            .filter { it.isAnnotationPresent(ChainStart::class.java) }
            .forEach { method ->
                val chainStart = method.getAnnotation(ChainStart::class.java)

                val genericReturn = method.genericReturnType
                require(genericReturn is ParameterizedType && genericReturn.rawType == ChainResult::class.java) {
                    "@ChainStart method '${method.name}' must return ChainResult<*>, found ${method.returnType.simpleName}"
                }

                val node = this[chainStart.node.java.name]
                    ?: error("@ChainStart method '${method.name}' references unknown node: ${chainStart.node.simpleName}")

                val declaredResultType = genericReturn.actualTypeArguments[0] as? Class<*>
                if (declaredResultType != null) {
                    val lastNode = lastNode(node.id)
                    require(declaredResultType.isAssignableFrom(lastNode.outputType)) {
                        "@ChainStart method '${method.name}' declares ChainResult<${declaredResultType.simpleName}> " +
                                "but chain ending at '${lastNode.id}' outputs ChainResult<${lastNode.outputType.simpleName}>"
                    }
                }
            }
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
