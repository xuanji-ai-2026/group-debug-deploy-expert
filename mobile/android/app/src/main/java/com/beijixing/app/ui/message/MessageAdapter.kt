package com.beijixing.app.ui.message

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.Message

class MessageAdapter(
    private val onItemClick: (Message) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages: List<Message> = emptyList()

    fun submitList(list: List<Message>) {
        messages = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
        holder.itemView.setOnClickListener { onItemClick(messages[position]) }
    }

    override fun getItemCount(): Int = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(android.R.id.text1) ?: TextView(itemView.context)
        private val tvContent: TextView = itemView.findViewById(android.R.id.text2) ?: TextView(itemView.context)

        fun bind(message: Message) {
            tvTitle.text = message.title
            tvContent.text = message.content.ifEmpty { "暂无内容" }
        }
    }
}
