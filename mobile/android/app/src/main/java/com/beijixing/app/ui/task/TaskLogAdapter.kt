package com.beijixing.app.ui.task

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.TaskLog

/**
 * 任务日志适配器
 *
 * @author 潘桂英（EMP-MOBILE-001）
 */
class TaskLogAdapter : ListAdapter<TaskLog, TaskLogAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_task_log, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvAction: TextView = itemView.findViewById(R.id.tvAction)
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

        fun bind(log: TaskLog) {
            tvTime.text = log.createTime
            tvAction.text = log.action
            tvMessage.text = log.message
            tvStatus.text = log.status

            // 状态颜色
            val statusColor = when (log.status) {
                "SUCCESS" -> R.color.status_success
                "FAILED" -> R.color.status_failed
                "WARNING" -> R.color.status_warning
                else -> R.color.text_secondary
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<TaskLog>() {
        override fun areItemsTheSame(oldItem: TaskLog, newItem: TaskLog): Boolean {
            return oldItem.logId == newItem.logId
        }

        override fun areContentsTheSame(oldItem: TaskLog, newItem: TaskLog): Boolean {
            return oldItem == newItem
        }
    }
}