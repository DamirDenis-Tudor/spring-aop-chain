package io.github.damir.denis.tudor.spring.aop.chain

import io.github.damir.denis.tudor.spring.aop.chain.aspect.Chainable
import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainNode
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

class ChainExecutor(@PublishedApi internal val chainMap: Map<String, ChainNode>) {

    @PublishedApi
    internal val logger = LoggerFactory.getLogger(ChainExecutor::class.java)

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

        return runChain(node, input) as O
    }

    @PublishedApi
    internal fun runChain(node: ChainNode, input: Any?): Any? {
        val chain = generateSequence(node) { it.nextId?.let(chainMap::get) }.toList()

        var result = input
        val log = buildString {
            append("Chain execution:")
            chain.forEachIndexed { index, step ->
                val stepInput = result.toString().take(100)
                result = step.invoke(result)
                val stepOutput = result.toString().take(100)
                if (index == 0) {
                    append("\n${step.id}: [input=$stepInput, output=$stepOutput]")
                } else {
                    append("\n   -> ${step.id}: [input=$stepInput, output=$stepOutput]")
                }
            }
        }
        logger.info(log)
        return result
    }
}
