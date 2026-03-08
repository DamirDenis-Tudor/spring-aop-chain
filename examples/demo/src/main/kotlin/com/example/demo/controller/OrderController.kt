package com.example.demo.controller

import com.example.demo.shipping.OrderReceipt
import com.example.demo.validation.OrderRequest
import com.example.demo.validation.OrderValidationService
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStart
import io.github.damir.denis.tudor.spring.aop.chain.registry.ChainResult
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/orders")
open class OrderController {

    // POST /api/orders
    @PostMapping
    @ChainStart(node = OrderValidationService::class)
    open fun placeOrder(@RequestBody request: OrderRequest): ChainResult<OrderReceipt> = ChainResult.Pending

    @ExceptionHandler(Exception::class)
    fun handleError(ex: Exception): ResponseEntity<Map<String, String>> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(mapOf("error" to (ex.message ?: "Unknown error")))
}
