package com.beijixing.app.data.model

data class RechargeOrder(
    val id: Long = 0,
    val orderNo: String = "",
    val amount: Double = 0.0,
    val points: Int = 0,
    val giftPoints: Int = 0,
    val payMethod: String = "WECHAT",
    val status: String = "PENDING",
    val createTime: String = ""
) {
    fun getStatusText(): String {
        return when (status) {
            "PAID" -> "已支付"
            "PENDING" -> "待支付"
            else -> status
        }
    }
    
    fun getStatusColor(): Int {
        return when (status) {
            "PAID" -> android.graphics.Color.GREEN
            else -> android.graphics.Color.GRAY
        }
    }
    
    fun getPayMethodText(): String {
        return when (payMethod) {
            "WECHAT" -> "微信支付"
            "ALIPAY" -> "支付宝"
            else -> payMethod
        }
    }
    
    fun getTotalPoints(): Int = points + giftPoints
}
