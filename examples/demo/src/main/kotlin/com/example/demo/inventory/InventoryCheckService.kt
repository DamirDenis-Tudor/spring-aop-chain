package com.example.demo.inventory

import com.example.demo.pricing.PricingService
import com.example.demo.validation.ValidatedOrder
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@ChainStep(next = PricingService::class)
open class InventoryCheckService : Chainable<ValidatedOrder, ReservedOrder> {

    private val log = LoggerFactory.getLogger(javaClass)

    private val stock = mutableMapOf(
        "LAPTOP-01" to 10, "MOUSE-02" to 50, "KB-03" to 30,
        "MONITOR-04" to 5, "HEADSET-05" to 20
    )

    override fun proceed(input: ValidatedOrder): ReservedOrder {
        val reserved = mutableMapOf<String, Int>()

        input.items.forEach { item ->
            val available = stock[item.productId]
                ?: throw IllegalStateException("Unknown product: ${item.productId}")
            require(available >= item.quantity) {
                "Insufficient stock for ${item.name}: requested=${item.quantity}, available=$available"
            }
            stock[item.productId] = available - item.quantity
            reserved[item.productId] = item.quantity
        }

        log.info("Stock reserved for order={}: {}", input.orderId, reserved)

        return ReservedOrder(
            orderId = input.orderId,
            customerId = input.customerId,
            items = input.items,
            shippingAddress = input.shippingAddress,
            promoCode = input.promoCode,
            reservedStock = reserved
        )
    }
}
