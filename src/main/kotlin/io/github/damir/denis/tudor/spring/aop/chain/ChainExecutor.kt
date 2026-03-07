package io.github.damir.denis.tudor.spring.aop.chain

import io.github.damir.denis.tudor.spring.aop.chain.aspect.Chainable
import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainNode
import kotlin.reflect.KClass

class ChainExecutor(@PublishedApi internal val chainMap: Map<String, ChainNode>) {

    @Suppress("UNCHECKED_CAST")
    inline fun <reified I, reified O> execute(startNode: KClass<out Chainable<I, *>>, input: I): O {
        val node = chainMap[startNode.java.name]
            ?: error("No chain node found for class: ${startNode.simpleName}")

        if (!node.inputType.isAssignableFrom(I::class.java)) {
            error(
                "Chain input type mismatch: provided ${I::class.simpleName} " +
                        "but '${node.id}' expects ${node.inputType.simpleName}"
            )
        }

        val lastNode = generateSequence(node) { it.nextId?.let(chainMap::get) }.last()

        if (lastNode.outputType != O::class.java) {
            error(
                "Chain output type mismatch: expected ${O::class.simpleName} " +
                        "but chain ending at '${lastNode.id}' outputs ${lastNode.outputType.simpleName}"
            )
        }

        var result: Any? = input
        generateSequence(node) { it.nextId?.let(chainMap::get) }.forEach { result = it.invoke(result) }
        return result as O
    }
}
