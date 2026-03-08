package com.example.demo.pricing

import com.example.demo.inventory.ReservedOrder
import com.example.demo.payment.PaymentService
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.math.roundToInt

@Service
@ChainStep(next = PaymentService::class)
open class PricingService : Chainable<ReservedOrder, PricedOrder> {

    private val log = LoggerFactory.getLogger(javaClass)
    private val taxRate = 0.19
    private val promoCodes = mapOf("SAVE10" to 0.10, "SAVE20" to 0.20, "VIP50" to 0.50)

    override fun proceed(input: ReservedOrder): PricedOrder {
        val subtotal = input.items.sumOf { it.unitPrice * it.quantity }
        val discountRate = input.promoCode?.let { promoCodes[it.uppercase()] } ?: 0.0
        val discount = (subtotal * discountRate * 100).roundToInt() / 100.0
        val tax = ((subtotal - discount) * taxRate * 100).roundToInt() / 100.0
        val total = subtotal - discount + tax

        log.info("Order={} priced: subtotal={}, discount={}, tax={}, total={}",
            input.orderId, subtotal, discount, tax, total)

        return PricedOrder(
            orderId = input.orderId,
            customerId = input.customerId,
            items = input.items,
            shippingAddress = input.shippingAddress,
            subtotal = subtotal,
            discount = discount,
            tax = tax,
            total = total
        )
    }
}
