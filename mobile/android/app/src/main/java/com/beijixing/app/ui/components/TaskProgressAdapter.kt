package com.beijixing.app.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskProgressAdapter(
    private val onItemClick: (Any) -> Unit,
    private val onPause: (Any) -> Unit = {},
    private val onResume: (Any) -> Unit = {}
) : RecyclerView.Adapter<TaskProgressAdapter.TaskViewHolder>() {

    private var taskList: List<Any> = emptyList()

    fun submitList(list: List<Any>) {
        taskList = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(taskList[position], position)
        holder.itemView.setOnClickListener { onItemClick(taskList[position]) }
    }

    override fun getItemCount(): Int = taskList.size

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(android.R.id.text1) ?: TextView(itemView.context)
        private val tvStatus: TextView = itemView.findViewById(android.R.id.text2) ?: TextView(itemView.context)

        fun bind(@Suppress("UNUSED_PARAMETER") item: Any, index: Int) {
            tvName.text = "任务 #$index"
            tvStatus.text = "运行中"
        }
    }
}
