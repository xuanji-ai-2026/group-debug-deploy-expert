package com.beijixing.app.ui.task

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.Task
import com.beijixing.app.ui.components.TaskProgressAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskActivity : AppCompatActivity() {

    private val viewModel: TaskViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TaskProgressAdapter
    private lateinit var progressBar: View
    private lateinit var emptyView: View
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    
    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TYPE = "task_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        setupRefreshLayout()
        observeState()
        
        viewModel.refresh()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView) ?: RecyclerView(this)
        progressBar = findViewById(R.id.progressBar) ?: View(this)
        emptyView = findViewById(R.id.emptyView) ?: View(this)
        swipeRefresh = findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeRefresh)
            ?: androidx.swiperefreshlayout.widget.SwipeRefreshLayout(this).apply { isEnabled = false }
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "任务管理"
            it.setNavigationOnClickListener { finish() }
            
            it.inflateMenu(R.menu.menu_task)
            it.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_create_acquire -> navigateToCreateTask("ACQUIRE")
                    R.id.action_create_intercept -> navigateToCreateTask("INTERCEPT")
                    R.id.action_refresh -> viewModel.refresh()
                }
                true
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = TaskProgressAdapter(
            onItemClick = { task -> 
                val taskObj = task as? com.beijixing.app.data.model.Task
                navigateToDetail(taskObj?.taskId ?: 0L, taskObj?.type ?: "ACQUIRE") 
            },
            onPause = { task -> 
                val taskObj = task as? com.beijixing.app.data.model.Task
                showConfirmDialog("暂停任务", "确定要暂停任务「${taskObj?.name}」吗？") { 
                    viewModel.pauseTask(taskObj?.taskId ?: 0L) 
                }
            },
            onResume = { task -> 
                val taskObj = task as? com.beijixing.app.data.model.Task
                viewModel.resumeTask(taskObj?.taskId ?: 0L)
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TaskActivity)
            adapter = this@TaskActivity.adapter
        }
    }
    
    private fun setupRefreshLayout() {
        swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
            swipeRefresh.isRefreshing = false
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUI(state)
                }
            }
        }
    }
    
    private fun updateUI(state: TaskUiState) {
        progressBar.visibility = if (state.isLoading && !state.isRefreshing) View.VISIBLE else View.GONE
        
        if (state.tasks.isNotEmpty()) {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(state.tasks)
        } else if (!state.isLoading) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
        
        state.errorMsg?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
        
        state.successMsg?.let { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    private fun navigateToDetail(taskId: Long, type: String) {
        try {
            Intent(this, Class.forName("com.beijixing.app.ui.task.TaskDetailActivity")).apply {
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TASK_TYPE, type)
                startActivity(this)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "页面跳转失败", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun navigateToCreateTask(type: String) {
        try {
            val activityClass = when (type) {
                "ACQUIRE" -> Class.forName("com.beijixing.app.ui.acquire.CreateAcquireTaskActivity")
                else -> Class.forName("com.beijixing.app.ui.intercept.CreateInterceptTaskActivity")
            }
            Intent(this, activityClass).also { startActivity(it) }
        } catch (e: Exception) {
            Toast.makeText(this, "创建任务页面开发中", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消", null)
            .show()
    }
}
