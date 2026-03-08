package io.github.damir.denis.tudor.spring.aop.chain.aspect

import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainExecutor
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.aop.support.AopUtils

@Aspect
internal class ChainAspect(private val executor: ChainExecutor) {

    @Around(
        "execution(* io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable.proceed(..)) && target(bean)"
    )
    fun handleChain(pjp: ProceedingJoinPoint, bean: Any): Any? {
        val targetClass = AopUtils.getTargetClass(bean)

        val node = executor.chainMap[targetClass.name]
            ?: return pjp.proceed()

        return executor.runChain(node, pjp.args[0]).result
    }
}
