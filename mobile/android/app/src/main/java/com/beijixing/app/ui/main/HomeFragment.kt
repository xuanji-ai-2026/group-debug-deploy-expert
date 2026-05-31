package com.beijixing.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.DashboardData
import com.beijixing.app.data.model.Lead
import com.beijixing.app.data.model.Task
import com.beijixing.app.data.model.User
import com.beijixing.app.data.repository.LeadRepository
import com.beijixing.app.data.repository.TaskRepository
import com.beijixing.app.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
        
        fun newInstance(): HomeFragment {
            return HomeFragment()
        }
    }

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var taskRepository: TaskRepository

    @Inject
    lateinit var leadRepository: LeadRepository

    private lateinit var tvWelcome: TextView
    private lateinit var tvDateInfo: TextView
    private lateinit var tvTotalLeads: TextView
    private lateinit var tvTodayLeads: TextView
    private lateinit var tvActiveTasks: TextView
    private lateinit var tvAccountStatus: TextView
    private lateinit var tvPoints: TextView
    private lateinit var rvRecentLeads: RecyclerView
    private lateinit var rvRunningTasks: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initView(view)
        loadRealData()
        setupClickListeners()
    }

    private fun initView(view: View) {
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvDateInfo = view.findViewById(R.id.tvDateInfo)
        tvTotalLeads = view.findViewById(R.id.tvTotalLeads)
        tvTodayLeads = view.findViewById(R.id.tvTodayLeads)
        tvActiveTasks = view.findViewById(R.id.tvActiveTasks)
        tvAccountStatus = view.findViewById(R.id.tvAccountStatus)
        tvPoints = view.findViewById(R.id.tvPoints)
        rvRecentLeads = view.findViewById(R.id.rvRecentLeads)
        rvRunningTasks = view.findViewById(R.id.rvRunningTasks)

        rvRecentLeads.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
        rvRunningTasks.layoutManager = LinearLayoutManager(context)
    }

    private fun loadRealData() {
        lifecycleScope.launch {
            try {
                // 并行加载用户信息、仪表盘数据、任务列表、商机列表
                val userDeferred = lifecycleScope.launch { userRepository.getUserInfo() }
                val dashboardDeferred = lifecycleScope.launch { loadDashboardData() }
                val tasksDeferred = lifecycleScope.launch { loadTaskData() }
                val leadsDeferred = lifecycleScope.launch { loadLeadData() }

                // 等待用户信息加载完成
                userDeferred.join()
            } catch (e: Exception) {
                showError("加载数据失败：${e.message}")
            }
        }
    }

    private suspend fun loadDashboardData() {
        try {
            val response = leadRepository.getLeadStats()
            if (response.isSuccess) {
                val stats = response.getOrThrow()

                tvTotalLeads.text = "${stats.total ?: 0}"
                tvTodayLeads.text = "今日 +${stats.newCount ?: 0}"

                tvPoints.text = "0"
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "加载仪表盘数据失败", e)
        }
    }

    private suspend fun loadTaskData() {
        try {
            val result = taskRepository.getTasks(page = 1, size = 10, status = "RUNNING")
            if (result.isSuccess) {
                val pageResult = result.getOrThrow()
                val tasks = pageResult.records
                
                val runningTasks = tasks.filter { it.status == "RUNNING" }
                val pausedTasks = tasks.filter { it.status == "PAUSED" }
                tvActiveTasks.text = "${runningTasks.size + pausedTasks.size}"
                
                rvRunningTasks.adapter = TaskProgressAdapter(tasks)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "加载任务数据失败", e)
        }
    }

    private suspend fun loadLeadData() {
        try {
            val result = leadRepository.getLeads(page = 1, size = 5)
            if (result.isSuccess) {
                val pageResult = result.getOrThrow()
                rvRecentLeads.adapter = LeadCardAdapter(pageResult.records)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "加载商机数据失败", e)
        }
    }

    private suspend fun loadUserInfo() {
        try {
            val result = userRepository.getUserInfo()
            if (result.isSuccess) {
                val user = result.getOrThrow()
                
                val hour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toInt()
                val greeting = when {
                    hour < 6 -> "夜深了"
                    hour < 12 -> "上午好"
                    hour < 14 -> "中午好"
                    hour < 18 -> "下午好"
                    else -> "晚上好"
                }
                tvWelcome.text = "$greeting，${user.nickname} 👋"

                val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA)
                tvDateInfo.text = dateFormat.format(Date())
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "加载用户信息失败", e)
            tvWelcome.text = "您好，用户 👋"
            
            val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINA)
            tvDateInfo.text = dateFormat.format(Date())
        }
    }

    private fun setupClickListeners() {
        view?.findViewById<LinearLayout>(R.id.btnQuickIntercept)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击'截客任务'")
            try {
                val intent = android.content.Intent(context, com.beijixing.app.ui.intercept.CreateInterceptTaskActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动截客任务失败", e)
                showToast("功能开发中...")
            }
        }
        view?.findViewById<LinearLayout>(R.id.btnQuickCapture)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击'主动获客'")
            try {
                val intent = android.content.Intent(context, com.beijixing.app.ui.acquire.CreateAcquireTaskActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动主动获客失败", e)
                showToast("功能开发中...")
            }
        }
        view?.findViewById<LinearLayout>(R.id.btnQuickPublish)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击'内容发布'")
            try {
                val intent = android.content.Intent(context, com.beijixing.app.ui.crawl.CrawlManagementActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动内容发布失败", e)
                showToast("功能开发中...")
            }
        }
        view?.findViewById<LinearLayout>(R.id.btnQuickMessage)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击'批量私信'")
            try {
                val intent = android.content.Intent(context, com.beijixing.app.ui.crawl.SendMessageActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "启动批量私信失败", e)
                showToast("功能开发中...")
            }
        }
        view?.findViewById<TextView>(R.id.tvViewAllLeads)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击'查看全部商机'")
            try {
                val intent = android.content.Intent(context, com.beijixing.app.ui.lead.LeadListActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "跳转商机列表失败", e)
            }
        }
        view?.findViewById<TextView>(R.id.tvViewAllTasks)?.setOnClickListener {
            android.util.Log.i(TAG, "→ 用户点击'查看全部任务'")
            try {
                val intent = android.content.Intent(context, com.beijixing.app.ui.task.TaskActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "跳转任务中心失败", e)
            }
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private inner class LeadCardAdapter(private val leads: List<Lead>) : RecyclerView.Adapter<LeadCardAdapter.LeadViewHolder>() {

        inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.leadName)
            val tvCompany: TextView = itemView.findViewById(R.id.leadCompany)
            val tvSource: TextView = itemView.findViewById(R.id.leadSource)
            val tvLevel: TextView = itemView.findViewById(R.id.leadLevel)
            val tvNeeds: TextView = itemView.findViewById(R.id.leadNeeds)
            val tvTime: TextView = itemView.findViewById(R.id.leadTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_lead_card, parent, false)
            return LeadViewHolder(view)
        }

        override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
            val lead = leads[position]
            holder.tvName.text = lead.name
            holder.tvCompany.text = lead.companyName ?: "个人客户"
            holder.tvSource.text = lead.getSourceName()
            holder.tvLevel.text = lead.getLevelText()
            holder.tvNeeds.text = lead.needs
            holder.tvTime.text = lead.createTime
            
            val levelColorRes = when (lead.level) {
                "HIGH" -> R.color.lead_level_high
                "MEDIUM" -> R.color.warning_color
                else -> R.color.text_secondary
            }
            holder.tvLevel.setTextColor(resources.getColor(levelColorRes, null))
        }

        override fun getItemCount(): Int = leads.size
    }

    private inner class TaskProgressAdapter(private val tasks: List<Task>) : RecyclerView.Adapter<TaskProgressAdapter.TaskViewHolder>() {

        inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvTaskName: TextView = itemView.findViewById(R.id.taskName)
            val tvTaskType: TextView = itemView.findViewById(R.id.taskType)
            val tvPlatforms: TextView = itemView.findViewById(R.id.taskPlatforms)
            val progressBar: androidx.core.widget.ContentLoadingProgressBar = itemView.findViewById(R.id.progressBar)
            val tvProgress: TextView = itemView.findViewById(R.id.taskProgressText)
            val tvStats: TextView = itemView.findViewById(R.id.taskStats)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_task_progress, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = tasks[position]
            holder.tvTaskName.text = task.name
            holder.tvTaskType.text = task.getTypeText()
            holder.tvPlatforms.text = task.getPlatformNames()
            holder.progressBar.progress = task.progress
            holder.tvProgress.text = "${task.progress}%"
            holder.tvStats.text = "${task.completedCount}/${task.totalCount} (${task.successCount}成功)"
            
            val statusColorRes = when (task.status) {
                "RUNNING" -> R.color.status_running
                "PAUSED" -> R.color.status_pending
                else -> R.color.text_secondary
            }
            holder.tvProgress.setTextColor(resources.getColor(statusColorRes, null))
        }

        override fun getItemCount(): Int = tasks.size
    }
}
