package com.beijixing.app.ui.acquire

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.beijixing.app.R
import com.beijixing.app.data.model.CreateAcquireTaskRequest
import com.beijixing.app.data.repository.TaskRepository
import com.beijixing.app.databinding.ActivityCreateAcquireTaskBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CreateAcquireTaskActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateAcquireTaskBinding
    
    @Inject
    lateinit var taskRepository: TaskRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAcquireTaskBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initToolbar()
        setupClickListeners()
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "创建获客任务"
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

        val channel = when (binding.rgChannel.checkedRadioButtonId) {
            R.id.rbSearch -> "SEARCH"
            R.id.rbTopic -> "TOPIC"
            R.id.rbLocation -> "LOCATION"
            R.id.rbRecommend -> "RECOMMEND"
            else -> "SEARCH"
        }

        val targetPlatforms = mutableListOf<String>().apply {
            if (binding.cbDouyin.isChecked) add("DOUYIN")
            if (binding.cbXiaohongshu.isChecked) add("XIAOHONGSHU")
            if (binding.cbKuaishou.isChecked) add("KUAISHOU")
        }

        if (targetPlatforms.isEmpty()) {
            Toast.makeText(this, "请至少选择一个目标平台", Toast.LENGTH_SHORT).show()
            return
        }

        val keywords = keywordsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        submitCreateTask(
            name = taskName,
            channel = channel,
            targetPlatforms = targetPlatforms,
            keywords = keywords,
            dailyLimit = dailyLimit
        )
    }

    private fun submitCreateTask(
        name: String,
        channel: String,
        targetPlatforms: List<String>,
        keywords: List<String>,
        dailyLimit: Int
    ) {
        showLoading(true)

        val request = CreateAcquireTaskRequest(
            name = name,
            channel = channel,
            targetPlatforms = targetPlatforms,
            keywords = keywords,
            dailyLimit = dailyLimit
        )

        android.util.Log.d("TaskCreation", "=== 开始创建获客任务 ===")
        android.util.Log.d("TaskCreation", "任务名称: $name")
        android.util.Log.d("TaskCreation", "获客渠道: $channel")
        android.util.Log.d("TaskCreation", "目标平台: $targetPlatforms")
        android.util.Log.d("TaskCreation", "关键词: $keywords")
        android.util.Log.d("TaskCreation", "每日上限: $dailyLimit")

        lifecycleScope.launch {
            try {
                android.util.Log.d("TaskCreation", "调用TaskRepository.createAcquireTask()...")
                val result = taskRepository.createAcquireTask(request)

                showLoading(false)

                android.util.Log.d("TaskCreation", "API返回结果: $result")

                result.fold(
                    onSuccess = { task ->
                        android.util.Log.i("TaskCreation", "✅ 获客任务创建成功！任务ID: ${task.taskId}, 任务名: ${task.name}")
                        Toast.makeText(
                            this@CreateAcquireTaskActivity,
                            "获客任务创建成功！",
                            Toast.LENGTH_SHORT
                        ).show()

                        setResult(RESULT_OK)
                        finish()
                    },
                    onFailure = { error ->
                        android.util.Log.e("TaskCreation", "❌ 获客任务创建失败", error)
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
                            this@CreateAcquireTaskActivity,
                            "创建失败: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            } catch (e: Exception) {
                showLoading(false)
                android.util.Log.e("TaskCreation", "💥 创建获客任务时发生异常", e)
                android.util.Log.e("TaskCreation", "异常类型: ${e.javaClass.simpleName}")
                android.util.Log.e("TaskCreation", "异常消息: ${e.message}")
                android.util.Log.e("TaskCreation", "异常堆栈:", e)
                
                Toast.makeText(
                    this@CreateAcquireTaskActivity,
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
        const val REQUEST_CODE_CREATE_ACQUIRE_TASK = 2002
    }
}
