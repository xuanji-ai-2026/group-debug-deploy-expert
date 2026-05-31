package com.beijixing.app.ui.recharge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RechargePackageAdapter(
    private val onItemClick: (Any) -> Unit
) : RecyclerView.Adapter<RechargePackageAdapter.PackageViewHolder>() {

    private var packageList: List<Any> = emptyList()

    fun submitList(list: List<Any>) {
        packageList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(packageList[position], position)
        holder.itemView.setOnClickListener { onItemClick(packageList[position]) }
    }

    override fun getItemCount(): Int = packageList.size

    class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(android.R.id.text1) ?: TextView(itemView.context)

        fun bind(@Suppress("UNUSED_PARAMETER") item: Any, index: Int) {
            tvName.text = "套餐 #$index"
        }
    }
}
