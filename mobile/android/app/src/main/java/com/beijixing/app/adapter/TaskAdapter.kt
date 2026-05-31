package com.beijixing.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.CrawlTask

class TaskAdapter(
    private var tasks: List<CrawlTask> = emptyList(),
    private val onItemClickListener: OnItemClickListener? = null
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(task: CrawlTask)
        fun onAnalyzeClick(task: CrawlTask)
        fun onGenerateLeadsClick(task: CrawlTask)
        fun onPauseClick(task: CrawlTask)
        fun onStopClick(task: CrawlTask)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTaskName: TextView = itemView.findViewById(R.id.tv_task_name)
        val tvPlatform: TextView = itemView.findViewById(R.id.tv_platform)
        val tvStatus: TextView = itemView.findViewById(R.id.tv_status)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progress_task)
        val tvStatistics: TextView = itemView.findViewById(R.id.tv_statistics)
        val tvCreateTime: TextView = itemView.findViewById(R.id.tv_create_time)
        val btnAnalyze: View = itemView.findViewById(R.id.btn_analyze)
        val btnGenerateLeads: View = itemView.findViewById(R.id.btn_generate_leads)
        val btnPause: View = itemView.findViewById(R.id.btn_pause)
        val btnStop: View = itemView.findViewById(R.id.btn_stop)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_crawl_task, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]

        holder.tvTaskName.text = task.taskName ?: "未命名任务"
        holder.tvPlatform.text = getPlatformDisplayName(task.platformCode)
        holder.tvStatus.text = getStatusText(task.status)
        holder.tvStatus.setBackgroundResource(getStatusBackground(task.status))
        
        holder.progressBar.progress = task.progressPercent ?: 0
        
        val stats = StringBuilder()
        stats.append("${task.totalCommentsFound ?: 0}条评论 | ")
        stats.append("${task.highIntentCount ?: 0}高意向 | ")
        stats.append("${task.leadsGenerated ?: 0}商机")
        holder.tvStatistics.text = stats.toString()
        
        holder.tvCreateTime.text = task.createTime?.toString()?.substring(0, 16) ?: ""

        when (task.status) {
            1 -> {
                holder.btnPause.visibility = View.VISIBLE
                holder.btnStop.visibility = View.VISIBLE
                holder.btnAnalyze.visibility = View.GONE
                holder.btnGenerateLeads.visibility = View.GONE
            }
            2 -> {
                holder.btnPause.visibility = View.GONE
                holder.btnStop.visibility = View.GONE
                holder.btnAnalyze.visibility = View.VISIBLE
                holder.btnGenerateLeads.visibility = if ((task.highIntentCount ?: 0) > 0) View.VISIBLE else View.GONE
            }
            else -> {
                holder.btnPause.visibility = View.GONE
                holder.btnStop.visibility = View.GONE
                holder.btnAnalyze.visibility = View.GONE
                holder.btnGenerateLeads.visibility = View.GONE
            }
        }

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(task)
        }

        holder.btnAnalyze.setOnClickListener {
            onItemClickListener?.onAnalyzeClick(task)
        }

        holder.btnGenerateLeads.setOnClickListener {
            onItemClickListener?.onGenerateLeadsClick(task)
        }

        holder.btnPause.setOnClickListener {
            onItemClickListener?.onPauseClick(task)
        }

        holder.btnStop.setOnClickListener {
            onItemClickListener?.onStopClick(task)
        }
    }

    override fun getItemCount(): Int = tasks.size

    fun submitList(newTasks: List<CrawlTask>) {
        tasks = newTasks
        notifyDataSetChanged()
    }

    private fun getPlatformDisplayName(platformCode: String?): String {
        return when (platformCode?.uppercase()) {
            "DOUYIN" -> "抖音"
            "XIAOHONGSHU" -> "小红书"
            "KUAISHOU" -> "快手"
            "WEIBO" -> "微博"
            "BILIBILI" -> "B站"
            else -> platformCode ?: "未知"
        }
    }

    private fun getStatusText(status: Int?): String {
        return when (status) {
            0 -> "待执行"
            1 -> "进行中"
            2 -> "已完成"
            3 -> "失败"
            else -> "未知"
        }
    }

    private fun getStatusBackground(status: Int?): Int {
        return when (status) {
            0 -> R.drawable.bg_status_pending
            1 -> R.drawable.bg_status_running
            2 -> R.drawable.bg_status_completed
            3 -> R.drawable.bg_status_failed
            else -> R.drawable.bg_status_default
        }
    }
}
