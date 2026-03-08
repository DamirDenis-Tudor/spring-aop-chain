package io.github.damir.denis.tudor.spring.aop.chain.registry

import org.slf4j.LoggerFactory

class ChainExecutor(@PublishedApi internal val chainMap: Map<String, ChainNode>) {

    @PublishedApi
    internal val logger = LoggerFactory.getLogger(ChainExecutor::class.java)

    @PublishedApi
    internal fun runChain(node: ChainNode, input: Any?): ChainResult<Any?> {
        val chain = generateSequence(node) { it.nextId?.let(chainMap::get) }.toList()

        var result = input
        chain.forEach { step -> result = step.invoke(result) }

        logger.info(
            "Chain executed: {}",
            chain.joinToString(" -> ") { it.id }
        )

        return ChainResult.Success(result)
    }
}
