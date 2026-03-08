package com.example.demo.shipping

import com.example.demo.payment.PaidOrder
import io.github.damir.denis.tudor.spring.aop.chain.annotation.ChainStep
import io.github.damir.denis.tudor.spring.aop.chain.interfaces.Chainable
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@ChainStep
open class ShippingService : Chainable<PaidOrder, OrderReceipt> {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun proceed(input: PaidOrder): OrderReceipt {
        val trackingNumber = "TRACK-${UUID.randomUUID().toString().take(8).uppercase()}"
        val estimatedDelivery = LocalDate.now().plusDays(3 + input.items.size.toLong())

        log.info("Shipping scheduled for order={}: tracking={}, eta={}",
            input.orderId, trackingNumber, estimatedDelivery)

        return OrderReceipt(
            orderId = input.orderId,
            customerId = input.customerId,
            items = input.items,
            total = input.total,
            transactionId = input.transactionId,
            trackingNumber = trackingNumber,
            estimatedDelivery = estimatedDelivery.toString()
        )
    }
}
