package io.github.damir.denis.tudor.spring.aop.chain.aspect

interface Chainable<I, O> {
    fun proceed(input: I): O
}
