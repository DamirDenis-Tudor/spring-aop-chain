package com.example.demo.payment

import com.example.demo.model.OrderItem
import java.time.Instant

data class PaidOrder(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val total: Double,
    val transactionId: String,
    val paidAt: Instant = Instant.now()
)
