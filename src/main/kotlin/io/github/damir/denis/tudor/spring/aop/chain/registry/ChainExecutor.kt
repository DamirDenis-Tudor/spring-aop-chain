package io.github.damir.denis.tudor.spring.aop.chain.registry

import org.slf4j.LoggerFactory

class ChainExecutor(@PublishedApi internal val chainMap: Map<String, ChainNode>) {

    @PublishedApi
    internal val logger = LoggerFactory.getLogger(ChainExecutor::class.java)

    @PublishedApi
    internal fun runChain(node: ChainNode, input: Any?): ChainResult<Any?> {
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
        return ChainResult.Success(result)
    }
}
