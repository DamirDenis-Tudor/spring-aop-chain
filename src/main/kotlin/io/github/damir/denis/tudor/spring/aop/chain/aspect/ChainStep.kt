package io.github.damir.denis.tudor.spring.aop.chain.aspect

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ChainStep(val next: KClass<*> = Unit::class)
