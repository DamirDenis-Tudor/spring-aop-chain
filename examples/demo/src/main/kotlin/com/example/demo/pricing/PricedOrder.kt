package com.example.demo.pricing

import com.example.demo.model.OrderItem

data class PricedOrder(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val subtotal: Double,
    val discount: Double,
    val tax: Double,
    val total: Double
)
