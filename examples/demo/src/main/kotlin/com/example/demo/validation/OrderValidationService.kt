package com.example.demo.validation

import com.example.demo.inventory.InventoryCheckService
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@ChainStep(next = InventoryCheckService::class)
open class OrderValidationService : Chainable<OrderRequest, ValidatedOrder> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun proceed(input: OrderRequest): ValidatedOrder {
        require(input.items.isNotEmpty()) { "Order must have at least one item" }
        require(input.shippingAddress.isNotBlank()) { "Shipping address is required" }
        input.items.forEach {
            require(it.quantity > 0) { "Quantity must be positive for ${it.name}" }
            require(it.unitPrice > 0) { "Price must be positive for ${it.name}" }
        }

        log.info("Order validated for customer={}", input.customerId)

        return ValidatedOrder(
            customerId = input.customerId,
            items = input.items,
            shippingAddress = input.shippingAddress,
            promoCode = input.promoCode
        )
    }
}
