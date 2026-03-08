package com.example.demo.validation

import com.example.demo.model.OrderItem

data class OrderRequest(
    val customerId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val promoCode: String? = null
)
