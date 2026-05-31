package com.beijixing.app.ui.intercept

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beijixing.app.R
import com.beijixing.app.data.model.CreateInterceptTaskRequest
import com.beijixing.app.data.model.InterceptTask
import com.beijixing.app.data.repository.TaskRepository
import com.beijixing.app.databinding.ActivityCreateInterceptTaskBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CreateInterceptTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateInterceptTaskBinding
    
    @Inject
    lateinit var taskRepository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateInterceptTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        setupClickListeners()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "创建截客任务"
        }
        
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupClickListeners() {
        binding.btnSubmit.setOnClickListener {
            validateAndSubmit()
        }
    }

    private fun validateAndSubmit() {
        val taskName = binding.etTaskName.text.toString().trim()
        val keywordsStr = binding.etKeywords.text.toString().trim()
        val competitorAccountsStr = binding.etCompetitorAccounts.text.toString().trim()
        val dailyLimitStr = binding.etDailyLimit.text.toString().trim()

        if (taskName.isEmpty()) {
            binding.tilTaskName.error = "请输入任务名称"
            return
        }

        if (keywordsStr.isEmpty()) {
            binding.tilKeywords.error = "请输入至少一个关键词"
            return
        }

        if (dailyLimitStr.isEmpty()) {
            binding.tilDailyLimit.error = "请输入每日上限"
            return
        }

        val dailyLimit = dailyLimitStr.toIntOrNull()
        if (dailyLimit == null || dailyLimit <= 0) {
            binding.tilDailyLimit.error = "请输入有效的数字"
            return
        }

        val targetPlatform = when (binding.rgPlatform.checkedRadioButtonId) {
            R.id.rbDouyin -> "DOUYIN"
            R.id.rbXiaohongshu -> "XIAOHONGSHU"
            R.id.rbKuaishou -> "KUAISHOU"
            else -> "DOUYIN"
        }

        val targetType = when (binding.rgTargetType.checkedRadioButtonId) {
            R.id.rbComment -> "COMMENT"
            R.id.rbFan -> "FAN"
            R.id.rbSearch -> "SEARCH"
            else -> "COMMENT"
        }

        val keywords = keywordsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val competitorAccounts = competitorAccountsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        submitCreateTask(
            name = taskName,
            targetPlatform = targetPlatform,
            targetType = targetType,
            keywords = keywords,
            competitorAccounts = competitorAccounts,
            dailyLimit = dailyLimit
        )
    }

    private fun submitCreateTask(
        name: String,
        targetPlatform: String,
        targetType: String,
        keywords: List<String>,
        competitorAccounts: List<String>,
        dailyLimit: Int
    ) {
        showLoading(true)

        val request = CreateInterceptTaskRequest(
            name = name,
            targetPlatform = targetPlatform,
            targetType = targetType,
            keywords = keywords,
            competitorAccounts = competitorAccounts,
            dailyLimit = dailyLimit
        )

        android.util.Log.d("TaskCreation", "=== 开始创建截客任务 ===")
        android.util.Log.d("TaskCreation", "任务名称: $name")
        android.util.Log.d("TaskCreation", "目标平台: $targetPlatform")
        android.util.Log.d("TaskCreation", "目标类型: $targetType")
        android.util.Log.d("TaskCreation", "关键词: $keywords")
        android.util.Log.d("TaskCreation", "竞争对手账号: $competitorAccounts")
        android.util.Log.d("TaskCreation", "每日上限: $dailyLimit")

        lifecycleScope.launch {
            try {
                android.util.Log.d("TaskCreation", "调用TaskRepository.createInterceptTask()...")
                val result = taskRepository.createInterceptTask(request)

                showLoading(false)

                android.util.Log.d("TaskCreation", "API返回结果: $result")

                result.fold(
                    onSuccess = { task ->
                        android.util.Log.i("TaskCreation", "✅ 截客任务创建成功！任务ID: ${task.taskId}, 任务名: ${task.name}")
                        Toast.makeText(
                            this@CreateInterceptTaskActivity,
                            "截客任务创建成功！",
                            Toast.LENGTH_SHORT
                        ).show()

                        setResult(RESULT_OK)
                        finish()
                    },
                    onFailure = { error ->
                        android.util.Log.e("TaskCreation", "❌ 截客任务创建失败", error)
                        android.util.Log.e("TaskCreation", "错误类型: ${error.javaClass.simpleName}")
                        android.util.Log.e("TaskCreation", "错误消息: ${error.message}")
                        android.util.Log.e("TaskCreation", "错误堆栈:", error)
                        
                        val errorMessage = when {
                            error.message?.contains("Unable to resolve host", ignoreCase = true) == true -> "网络连接失败，请检查网络设置"
                            error.message?.contains("timeout", ignoreCase = true) == true -> "请求超时，请稍后重试"
                            error.message?.contains("401", ignoreCase = true) == true -> "登录已过期，请重新登录"
                            error.message?.contains("403", ignoreCase = true) == true -> "没有权限执行此操作"
                            error.message?.contains("404", ignoreCase = true) == true -> "API接口不存在，请联系管理员"
                            error.message?.contains("500", ignoreCase = true) == true -> "服务器内部错误，请稍后重试"
                            else -> error.message ?: "未知错误"
                        }
                        
                        Toast.makeText(
                            this@CreateInterceptTaskActivity,
                            "创建失败: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                android.util.Log.e("TaskCreation", "💥 创建截客任务时发生异常", e)
                android.util.Log.e("TaskCreation", "异常类型: ${e.javaClass.simpleName}")
                android.util.Log.e("TaskCreation", "异常消息: ${e.message}")
                android.util.Log.e("TaskCreation", "异常堆栈:", e)
                
                Toast.makeText(
                    this@CreateInterceptTaskActivity,
                    "网络异常: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            binding.btnSubmit.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
        } else {
            binding.btnSubmit.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val REQUEST_CODE_CREATE_INTERCEPT_TASK = 2001
    }
}
