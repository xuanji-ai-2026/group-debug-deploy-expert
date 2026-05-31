package com.beijixing.app.data.model

data class Account(
    val id: Long = 0,
    val platform: String = "",
    val accountName: String = "",
    val avatar: String? = null,
    val status: String = "ACTIVE"
)
