package com.beijixing.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.Task
import com.beijixing.app.data.model.TaskSummary
import com.beijixing.app.data.repository.TaskRepository
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TaskFragment : Fragment() {

    companion object {
        private const val TAG = "TaskFragment"
        
        fun newInstance(): TaskFragment {
            return TaskFragment()
        }
    }

    @Inject
    lateinit var taskRepository: TaskRepository

    private lateinit var tvTotalTasks: TextView
    private lateinit var tvRunningTasks: TextView
    private lateinit var tvCompletedTasks: TextView
    private lateinit var tvSuccessRate: TextView
    private lateinit var chipGroupType: ChipGroup
    private lateinit var tvTaskListTitle: TextView
    private lateinit var rvTaskList: RecyclerView

    private val allTasks = mutableListOf<Task>()
    private val filteredTasks = mutableListOf<Task>()
    private var currentTypeFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initView(view)
        loadRealData()
        setupListeners()
    }

    private fun initView(view: View) {
        tvTotalTasks = view.findViewById(R.id.tvTotalTasks)
        tvRunningTasks = view.findViewById(R.id.tvRunningTasks)
        tvCompletedTasks = view.findViewById(R.id.tvCompletedTasks)
        tvSuccessRate = view.findViewById(R.id.tvSuccessRate)
        chipGroupType = view.findViewById(R.id.chipGroupType)
        tvTaskListTitle = view.findViewById(R.id.tvTaskListTitle)
        rvTaskList = view.findViewById(R.id.rvTaskList)

        rvTaskList.layoutManager = LinearLayoutManager(context)
    }

    private fun loadRealData() {
        lifecycleScope.launch {
            try {
                // 加载任务列表
                val tasksResult = taskRepository.getTasks(page = 1, size = 50)
                if (tasksResult.isSuccess) {
                    val pageResult = tasksResult.getOrThrow()
                    allTasks.clear()
                    allTasks.addAll(pageResult.records)

                    filteredTasks.clear()
                    filteredTasks.addAll(allTasks)

                    // 从任务列表计算统计数据
                    updateTaskStatistics()

                    updateTaskList()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "加载任务数据失败", e)
                showError("加载失败：${e.message}")
            }
        }
    }

    private fun updateTaskStatistics() {
        val total = allTasks.size
        val running = allTasks.count { it.status == "RUNNING" }
        val completed = allTasks.count { it.status == "COMPLETED" }
        val successRate = if (total > 0) (completed * 100 / total) else 0

        tvTotalTasks.text = "$total"
        tvRunningTasks.text = "$running"
        tvCompletedTasks.text = "$completed"
        tvSuccessRate.text = "$successRate%"
    }

    private fun setupListeners() {
        chipGroupType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) {
                currentTypeFilter = null
            } else {
                val chipId = checkedIds.first()
                currentTypeFilter = when (chipId) {
                    R.id.chipIntercept -> "INTERCEPT"
                    R.id.chipCapture -> "ACTIVE_CAPTURE"
                    R.id.chipPublish -> "CONTENT_PUBLISH"
                    R.id.chipMessage -> "CUSTOM_MESSAGE"
                    else -> null
                }
            }
            filterTasks(currentTypeFilter)
        }

        // 注意：btnCreateTask 按钮在当前版本中暂未实现
    }

    private fun filterTasks(type: String?) {
        filteredTasks.clear()
        
        if (type == null) {
            filteredTasks.addAll(allTasks)
        } else {
            filteredTasks.addAll(allTasks.filter { it.type == type })
        }
        
        updateTaskList()
    }

    private fun updateTaskList() {
        tvTaskListTitle.text = "任务列表 (${filteredTasks.size})"
        
        if (rvTaskList.adapter == null) {
            rvTaskList.adapter = TaskListAdapter(filteredTasks) { task ->
                showToast("查看任务详情：${task.name}")
            }
        } else {
            (rvTaskList.adapter as? TaskListAdapter)?.updateData(filteredTasks)
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private inner class TaskListAdapter(
        private var tasks: List<Task>,
        private val onItemClick: (Task) -> Unit
    ) : RecyclerView.Adapter<TaskListAdapter.TaskViewHolder>() {

        inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.taskName)
            val tvType: TextView = itemView.findViewById(R.id.taskType)
            val tvPlatforms: TextView = itemView.findViewById(R.id.taskPlatforms)
            val tvProgress: TextView = itemView.findViewById(R.id.taskProgressText)
            val progressBar: androidx.core.widget.ContentLoadingProgressBar = itemView.findViewById(R.id.progressBar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_task_progress, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = tasks[position]
            holder.tvName.text = task.name
            holder.tvType.text = task.getTypeText()
            holder.tvPlatforms.text = task.getPlatformNames()
            holder.progressBar.progress = task.progress
            holder.tvProgress.text = "${task.progress}%"

            holder.itemView.setOnClickListener { onItemClick(task) }
        }

        override fun getItemCount(): Int = tasks.size

        fun updateData(newTasks: List<Task>) {
            tasks = newTasks
            notifyDataSetChanged()
        }
    }
}
