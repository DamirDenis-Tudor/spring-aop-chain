package io.github.damir.denis.tudor.spring.aop.chain.annotation

import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ChainStart(val node: KClass<out Chainable<*, *>>)
