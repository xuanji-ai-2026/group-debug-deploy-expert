package com.beijixing.app.ui.acquire

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
import com.beijixing.app.data.model.AcquireTask
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

/**
 * 获客任务列表适配器
 *
 * @author 潘桂英（EMP-MOBILE-001）
 */
class AcquireTaskAdapter(
    private val onItemClick: (AcquireTask) -> Unit,
    private val onPause: (AcquireTask) -> Unit,
    private val onResume: (AcquireTask) -> Unit,
    private val onStop: (AcquireTask) -> Unit,
    private val onStats: (AcquireTask) -> Unit
) : ListAdapter<AcquireTask, AcquireTaskAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_acquire_task, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvChannel: TextView = itemView.findViewById(R.id.tvChannel)
        private val tvPlatforms: TextView = itemView.findViewById(R.id.tvPlatforms)
        private val tvKeywords: TextView = itemView.findViewById(R.id.tvKeywords)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
        private val tvTodayCount: TextView = itemView.findViewById(R.id.tvTodayCount)
        private val tvTotalCount: TextView = itemView.findViewById(R.id.tvTotalCount)
        private val chipQuality: Chip = itemView.findViewById(R.id.chipQuality)
        private val btnPause: MaterialButton = itemView.findViewById(R.id.btnPause)
        private val btnResume: MaterialButton = itemView.findViewById(R.id.btnResume)
        private val btnStop: MaterialButton = itemView.findViewById(R.id.btnStop)
        private val btnStats: MaterialButton = itemView.findViewById(R.id.btnStats)
        private val ivChannel: ImageView = itemView.findViewById(R.id.ivChannel)

        fun bind(task: AcquireTask) {
            tvTaskName.text = task.name
            tvStatus.text = task.getStatusText()
            tvChannel.text = task.getChannelText()
            tvPlatforms.text = task.getPlatformNames()
            tvKeywords.text = task.keywords.take(3).joinToString(" · ") + if (task.keywords.size > 3) "..." else ""
            tvTodayCount.text = "${task.todayCount}"
            tvTotalCount.text = "${task.totalCount}"
            progressBar.progress = task.progress
            tvProgress.text = "${task.progress}%"

            // 质量评分
            chipQuality.text = "质量 ${task.leadQualityScore}"
            val qualityColor = when {
                task.leadQualityScore >= 80 -> R.color.quality_high
                task.leadQualityScore >= 60 -> R.color.quality_good
                task.leadQualityScore >= 40 -> R.color.quality_normal
                else -> R.color.quality_low
            }
            chipQuality.setChipBackgroundColorResource(qualityColor)

            // 渠道图标
            val channelIcon = when (task.channel) {
                "SEARCH" -> R.drawable.ic_search
                "TOPIC" -> R.drawable.ic_topic
                "LOCATION" -> R.drawable.ic_location
                "RECOMMEND" -> R.drawable.ic_recommend
                else -> R.drawable.ic_platform_default
            }
            ivChannel.setImageResource(channelIcon)

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
            btnStats.visibility = if (task.totalCount > 0) View.VISIBLE else View.GONE

            // 点击事件
            cardView.setOnClickListener { onItemClick(task) }
            btnPause.setOnClickListener { onPause(task) }
            btnResume.setOnClickListener { onResume(task) }
            btnStop.setOnClickListener { onStop(task) }
            btnStats.setOnClickListener { onStats(task) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AcquireTask>() {
        override fun areItemsTheSame(oldItem: AcquireTask, newItem: AcquireTask): Boolean {
            return oldItem.taskId == newItem.taskId
        }

        override fun areContentsTheSame(oldItem: AcquireTask, newItem: AcquireTask): Boolean {
            return oldItem == newItem
        }
    }
}