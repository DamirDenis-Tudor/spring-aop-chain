package io.github.damir.denis.tudor.spring.aop.chain.interfaces

interface Chainable<I, O> {
    fun proceed(input: I): O
}
