package com.beijixing.app.ui.intercept

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.InterceptTask
import com.google.android.material.button.MaterialButton

/**
 * 截客任务列表适配器
 *
 * @author 潘桂英（EMP-MOBILE-001）
 */
class InterceptTaskAdapter(
    private val onItemClick: (InterceptTask) -> Unit,
    private val onPause: (InterceptTask) -> Unit,
    private val onResume: (InterceptTask) -> Unit,
    private val onStop: (InterceptTask) -> Unit,
    private val onContact: (InterceptTask) -> Unit
) : ListAdapter<InterceptTask, InterceptTaskAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_intercept_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvPlatform: TextView = itemView.findViewById(R.id.tvPlatform)
        private val tvTargetType: TextView = itemView.findViewById(R.id.tvTargetType)
        private val tvKeywords: TextView = itemView.findViewById(R.id.tvKeywords)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        private val tvTodayCount: TextView = itemView.findViewById(R.id.tvTodayCount)
        private val tvTotalCount: TextView = itemView.findViewById(R.id.tvTotalCount)
        private val btnPause: MaterialButton = itemView.findViewById(R.id.btnPause)
        private val btnResume: MaterialButton = itemView.findViewById(R.id.btnResume)
        private val btnStop: MaterialButton = itemView.findViewById(R.id.btnStop)
        private val btnContact: MaterialButton = itemView.findViewById(R.id.btnContact)
        private val ivPlatform: ImageView = itemView.findViewById(R.id.ivPlatform)

        fun bind(task: InterceptTask) {
            tvTaskName.text = task.name
            tvStatus.text = task.getStatusText()
            tvPlatform.text = task.getPlatformText()
            tvTargetType.text = task.getTargetTypeText()
            tvKeywords.text = task.keywords.take(3).joinToString(" · ") + if (task.keywords.size > 3) "..." else ""
            tvTodayCount.text = "${task.todayCount}"
            tvTotalCount.text = "${task.totalCount}"
            progressBar.progress = task.progress
            tvProgress.text = "${task.progress}%"

            // 设置平台图标
            val platformIcon = when (task.targetPlatform) {
                "DOUYIN" -> R.drawable.ic_douyin
                "XIAOHONGSHU" -> R.drawable.ic_xiaohongshu
                else -> R.drawable.ic_platform_default
            }
            ivPlatform.setImageResource(platformIcon)

            // 状态颜色
            val statusColor = when (task.status) {
                "RUNNING" -> R.color.status_running
                "PAUSED" -> R.color.status_paused
                "COMPLETED" -> R.color.status_completed
                "FAILED" -> R.color.status_failed
                else -> R.color.status_pending
            }
            tvStatus.setTextColor(ContextCompat.getColor(itemView.context, statusColor))

            // 控制按钮可见性
            btnPause.visibility = if (task.canPause()) View.VISIBLE else View.GONE
            btnResume.visibility = if (task.canResume()) View.VISIBLE else View.GONE
            btnStop.visibility = if (task.canStop()) View.VISIBLE else View.GONE
            btnContact.visibility = if (task.interceptedLeads.isNotEmpty()) View.VISIBLE else View.GONE

            // 点击事件
            cardView.setOnClickListener { onItemClick(task) }
            btnPause.setOnClickListener { onPause(task) }
            btnResume.setOnClickListener { onResume(task) }
            btnStop.setOnClickListener { onStop(task) }
            btnContact.setOnClickListener { onContact(task) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<InterceptTask>() {
        override fun areItemsTheSame(oldItem: InterceptTask, newItem: InterceptTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: InterceptTask, newItem: InterceptTask): Boolean {
            return oldItem == newItem
        }
    }
}