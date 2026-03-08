package com.example.demo.payment

import com.example.demo.pricing.PricedOrder
import com.example.demo.shipping.ShippingService
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@ChainStep(next = ShippingService::class)
open class PaymentService : Chainable<PricedOrder, PaidOrder> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun proceed(input: PricedOrder): PaidOrder {
        require(input.total > 0) { "Cannot process payment for zero total" }

        val transactionId = "TXN-${UUID.randomUUID().toString().take(12).uppercase()}"
        log.info("Payment processed for order={}: txn={}, amount={}",
            input.orderId, transactionId, input.total)

        return PaidOrder(
            orderId = input.orderId,
            customerId = input.customerId,
            items = input.items,
            shippingAddress = input.shippingAddress,
            total = input.total,
            transactionId = transactionId
        )
    }
}
