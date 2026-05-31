package com.beijixing.app.ui.components

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.Lead

class LeadAdapter(
    private val onItemClick: (Lead) -> Unit,
    private val onItemDelete: (Lead) -> Unit = {}
) : RecyclerView.Adapter<LeadAdapter.LeadViewHolder>() {

    private var leadList: List<Lead> = emptyList()

    fun submitList(list: List<Lead>) {
        leadList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lead, parent, false)
        return LeadViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val lead = leadList[position]
        holder.bind(lead)
        holder.itemView.setOnClickListener { onItemClick(lead) }
    }

    override fun getItemCount(): Int = leadList.size

    class LeadViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvLevel: TextView = itemView.findViewById(R.id.tvLevel)
        private val tvNeed: TextView = itemView.findViewById(R.id.tvNeed)
        private val ivSource: ImageView = itemView.findViewById(R.id.ivSource)
        private val tvSource: TextView = itemView.findViewById(R.id.tvSource)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(lead: Lead) {
            tvName.text = lead.name
            tvLevel.text = lead.getLevelText()
            tvNeed.text = lead.needs
            tvSource.text = lead.getSourceName()
            tvTime.text = lead.createTime
            tvStatus.text = lead.getStatusText()
        }
    }
}
