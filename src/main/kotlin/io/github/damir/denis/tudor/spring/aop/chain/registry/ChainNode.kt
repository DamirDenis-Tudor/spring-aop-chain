package io.github.damir.denis.tudor.spring.aop.chain.registry

data class ChainNode(
    val id: String,
    val beanName: String,
    val nextId: String?,
    val inputType: Class<*>,
    val outputType: Class<*>,
    val invoke: (Any?) -> Any?
)
