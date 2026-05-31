package com.beijixing.app.ui.task

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TaskDetailActivity : AppCompatActivity() {

    private val viewModel: TaskDetailViewModel by viewModels()
    private lateinit var logAdapter: TaskLogAdapter

    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var progressBar: View
    private lateinit var viewOverview: View
    private lateinit var viewLogs: View
    private lateinit var viewStats: View
    private lateinit var recyclerViewLogs: RecyclerView
    private lateinit var tvEmptyLogs: TextView
    private lateinit var btnPause: Button
    private lateinit var btnResume: Button
    private lateinit var btnStop: Button
    private lateinit var btnEdit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_detail)

        initViews()
        
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0)
        if (taskId == 0L) {
            finish()
            return
        }

        viewModel.setTaskInfo(taskId, "GENERAL")
        initAdapter()
        observeState()
        viewModel.loadTaskDetail()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar) ?: return
        tabLayout = findViewById(R.id.tabLayout) ?: return
        progressBar = findViewById(R.id.progressBar) ?: View(this)
        viewOverview = findViewById(R.id.viewOverview) ?: View(this)
        viewLogs = findViewById(R.id.viewLogs) ?: View(this)
        viewStats = findViewById(R.id.viewStats) ?: View(this)
        recyclerViewLogs = findViewById(R.id.recyclerViewLogs) ?: RecyclerView(this)
        tvEmptyLogs = findViewById(R.id.tvEmptyLogs) ?: TextView(this)
        btnPause = findViewById(R.id.btnPause) ?: Button(this)
        btnResume = findViewById(R.id.btnResume) ?: Button(this)
        btnStop = findViewById(R.id.btnStop) ?: Button(this)
        btnEdit = findViewById(R.id.btnEdit) ?: Button(this)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }

        tabLayout.addTab(tabLayout.newTab().setText("概览"))
        tabLayout.addTab(tabLayout.newTab().setText("日志"))
        tabLayout.addTab(tabLayout.newTab().setText("统计"))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showOverview()
                    1 -> showLogs()
                    2 -> showStats()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        btnPause.setOnClickListener { viewModel.pauseTask() }
        btnResume.setOnClickListener { viewModel.resumeTask() }
        btnStop.setOnClickListener { showStopConfirmDialog() }
        btnEdit.setOnClickListener { showEditDialogSimplified() }
    }

    private fun initAdapter() {
        logAdapter = TaskLogAdapter()
        recyclerViewLogs.apply {
            layoutManager = LinearLayoutManager(this@TaskDetailActivity)
            adapter = logAdapter
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE

                    state.task?.let { task ->
                        supportActionBar?.title = task.name
                    }

                    logAdapter.submitList(state.logs)
                    tvEmptyLogs.visibility = if (state.logs.isEmpty()) View.VISIBLE else View.GONE

                    state.errorMsg?.let { msg ->
                        Snackbar.make(toolbar, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }

                    state.successMsg?.let { msg ->
                        Snackbar.make(toolbar, msg, Snackbar.LENGTH_SHORT).show()
                        viewModel.clearSuccess()
                    }
                }
            }
        }
    }

    private fun showOverview() {
        viewOverview.visibility = View.VISIBLE
        viewLogs.visibility = View.GONE
        viewStats.visibility = View.GONE
    }

    private fun showLogs() {
        viewOverview.visibility = View.GONE
        viewLogs.visibility = View.VISIBLE
        viewStats.visibility = View.GONE
        viewModel.loadLogs()
    }

    private fun showStats() {
        viewOverview.visibility = View.GONE
        viewLogs.visibility = View.GONE
        viewStats.visibility = View.VISIBLE
    }

    private fun showStopConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("确认停止")
            .setMessage("确定要停止此任务吗？")
            .setPositiveButton("是") { _, _ -> viewModel.stopTask() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showEditDialogSimplified() {
        val editText = EditText(this).apply {
            hint = "输入任务名称"
            setText(viewModel.currentTaskName)
        }

        AlertDialog.Builder(this)
            .setTitle("编辑任务")
            .setView(editText)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { dialog, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    viewModel.updateTaskName(newName)
                    Snackbar.make(toolbar, "已更新", Snackbar.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_task_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                viewModel.loadTaskDetail()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirmDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("确认删除")
            .setMessage("此操作不可撤销，继续吗？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteTask { finish() } }
            .setNegativeButton("取消", null)
            .show()
    }

    companion object {
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_TASK_TYPE = "task_type"
    }
}
