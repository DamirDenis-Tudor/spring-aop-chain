package io.github.damir.denis.tudor.spring.aop.chain.registry

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty

sealed class ChainResult<out T> {
    data class Success<T>(@JsonIgnore val value: T) : ChainResult<T>()
    object Pending : ChainResult<Nothing>()

    @get:JsonProperty("result")
    val result: T
        get() = when (this) {
            is Success -> value
            is Pending -> error("Chain has not been executed yet")
        }
}
