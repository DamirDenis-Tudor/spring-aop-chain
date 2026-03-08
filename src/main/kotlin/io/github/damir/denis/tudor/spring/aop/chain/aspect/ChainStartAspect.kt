package io.github.damir.denis.tudor.spring.aop.chain.aspect

import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStart
import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainExecutor
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect

@Aspect
internal class ChainStartAspect(private val executor: ChainExecutor) {

    @Around("@annotation(chainStart)")
    fun aroundChainStart(pjp: ProceedingJoinPoint, chainStart: ChainStart): Any? {
        val node = executor.chainMap[chainStart.node.java.name]
            ?: error("No chain node found for: ${chainStart.node.simpleName}")

        return executor.runChain(node, pjp.args[0])
    }
}
