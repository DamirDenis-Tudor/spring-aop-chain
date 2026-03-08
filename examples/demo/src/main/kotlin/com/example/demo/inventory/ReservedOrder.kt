package com.example.demo.inventory

import com.example.demo.model.OrderItem

data class ReservedOrder(
    val orderId: String,
    val customerId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val promoCode: String?,
    val reservedStock: Map<String, Int>
)
