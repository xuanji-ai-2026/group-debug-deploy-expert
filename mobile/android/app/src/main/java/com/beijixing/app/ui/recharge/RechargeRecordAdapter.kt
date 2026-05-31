package com.beijixing.app.ui.recharge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.RechargeOrder

class RechargeRecordAdapter : RecyclerView.Adapter<RechargeRecordAdapter.RecordViewHolder>() {

    private var records: List<RechargeOrder> = emptyList()

    fun submitList(list: List<RechargeOrder>) {
        records = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recharge_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvOrderNo: TextView = itemView.findViewById(R.id.tvOrderNo)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPoints: TextView = itemView.findViewById(R.id.tvPoints)
        private val tvPayMethod: TextView = itemView.findViewById(R.id.tvPayMethod)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)

        fun bind(order: RechargeOrder) {
            tvOrderNo.text = "订单号：${order.orderNo}"
            
            tvStatus.text = order.getStatusText()
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, order.getStatusColor()))
            
            tvPoints.text = "+${order.points}"
            if (order.giftPoints > 0) {
                tvPoints.text = "+${order.getTotalPoints()} (含赠送${order.giftPoints})"
            }
            
            tvPayMethod.text = order.getPayMethodText()
            tvAmount.text = "¥${"%.2f".format(order.amount)}"
            tvTime.text = order.createTime.ifEmpty { "时间未知" }
        }
    }
}
