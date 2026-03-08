package com.example.demo.validation

import com.example.demo.model.OrderItem
import java.time.Instant
import java.util.UUID

data class ValidatedOrder(
    val orderId: String = UUID.randomUUID().toString().take(8),
    val customerId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val promoCode: String?,
    val validatedAt: Instant = Instant.now()
)
