package com.beijixing.app.ui.account

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R

class AccountAdapter(
    private val onItemClick: (Any) -> Unit,
    private val onItemDelete: (Any) -> Unit = {}
) : RecyclerView.Adapter<AccountAdapter.AccountViewHolder>() {

    private var accounts: List<Any> = emptyList()

    fun submitList(list: List<Any>) {
        accounts = list
        notifyDataSetChanged()
    }

    fun getCurrentList(): List<Any> = accounts

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(accounts[position])
        holder.itemView.setOnClickListener { onItemClick(accounts[position]) }
    }

    override fun getItemCount(): Int = accounts.size

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(android.R.id.text1) ?: TextView(itemView.context)

        fun bind(account: Any) {
            tvName.text = account.toString()
        }
    }
}
