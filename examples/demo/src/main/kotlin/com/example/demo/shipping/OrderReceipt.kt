package com.example.demo.shipping

import com.example.demo.model.OrderItem
import java.time.Instant

data class OrderReceipt(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val total: Double,
    val transactionId: String,
    val trackingNumber: String,
    val estimatedDelivery: String,
    val processedAt: Instant = Instant.now()
)
