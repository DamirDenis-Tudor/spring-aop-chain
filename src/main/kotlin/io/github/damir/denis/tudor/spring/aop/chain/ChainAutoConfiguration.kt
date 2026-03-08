package io.github.damir.denis.tudor.spring.aop.chain

import io.github.damir.denis.tudor.spring.aop.chain.aspect.ChainAspect
import io.github.damir.denis.tudor.spring.aop.chain.aspect.ChainStartAspect
import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainExecutor
import io.github.damir.denis.tudor.spring.aop.chain.registry.buildChainMap
import io.github.damir.denis.tudor.spring.aop.chain.registry.detectChainCycles
import io.github.damir.denis.tudor.spring.aop.chain.registry.logChains
import io.github.damir.denis.tudor.spring.aop.chain.registry.validateChainStartMethods
import io.github.damir.denis.tudor.spring.aop.chain.registry.validateChainTypes
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class ChainAutoConfiguration {

    @Bean
    fun chainExecutor(ctx: ApplicationContext): ChainExecutor {
        val chainMap = ctx.buildChainMap()
        chainMap.validateChainTypes()
        chainMap.detectChainCycles()
        chainMap.validateChainStartMethods(ctx)
        chainMap.logChains()
        return ChainExecutor(chainMap)
    }

    @Bean
    fun chainAspect(executor: ChainExecutor) = ChainAspect(executor)

    @Bean
    fun chainStartAspect(executor: ChainExecutor) = ChainStartAspect(executor)
}
