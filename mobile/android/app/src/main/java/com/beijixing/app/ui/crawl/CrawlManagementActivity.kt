package com.beijixing.app.ui.crawl

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.beijixing.app.R
import com.beijixing.app.adapter.CommentAdapter
import com.beijixing.app.adapter.TaskAdapter
import com.beijixing.app.data.model.CrawlTask
import com.beijixing.app.data.model.SocialComment
import com.beijixing.app.databinding.ActivityCrawlManagementBinding
import com.beijixing.app.ui.crawl.viewmodel.CrawlViewModel
import com.beijixing.app.util.ToastUtil

class CrawlManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCrawlManagementBinding
    private val viewModel: CrawlViewModel by viewModels()
    private var taskAdapter: TaskAdapter? = null
    private var commentAdapter: CommentAdapter? = null
    
    private var currentTaskId: Long? = null
    private var selectedComments = mutableListOf<SocialComment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCrawlManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupObservers()
        loadTasks()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "获客中心"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.tabLayout.setupWithViewPager(binding.viewPager)

        val pagerAdapter = CrawlPagerAdapter(supportFragmentManager)
        binding.viewPager.adapter = pagerAdapter
        
        binding.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {}
            override fun onPageScrollStateChanged(state: Int) {}
        })

        binding.fabCreateTask.setOnClickListener {
            CreateTaskDialog.newInstance()
                .setOnTaskCreatedListener { request ->
                    viewModel.createTask(request)
                }
                .show(supportFragmentManager, "create_task")
        }
        
        setupTaskAdapter()
        setupCommentAdapter()
    }
    
    private fun setupTaskAdapter() {
        val clickListener = object : TaskAdapter.OnItemClickListener {
            override fun onItemClick(task: CrawlTask) {
                currentTaskId = task.id
                viewModel.loadComments(task.id ?: 0L)
                binding.viewPager.currentItem = 1
            }

            override fun onAnalyzeClick(task: CrawlTask) {
                viewModel.analyzeComments(task.id ?: 0L)
            }

            override fun onGenerateLeadsClick(task: CrawlTask) {
                showGenerateLeadsDialog(task)
            }

            override fun onPauseClick(task: CrawlTask) {
                viewModel.pauseTask(task.id ?: 0L)
            }

            override fun onStopClick(task: CrawlTask) {
                viewModel.stopTask(task.id ?: 0L)
            }
        }
        
        taskAdapter = TaskAdapter(emptyList(), clickListener)
    }
    
    private fun setupCommentAdapter() {
        val itemClickListener = object : CommentAdapter.OnItemClickListener {
            override fun onItemClick(comment: SocialComment, position: Int) {
                
            }
        }
        
        val messageClickListener = object : CommentAdapter.OnSendMessageClickListener {
            override fun onSendMessageClick(comment: SocialComment, position: Int) {
                startSendMessageActivity(comment)
            }
        }
        
        commentAdapter = CommentAdapter(emptyList(), itemClickListener, messageClickListener)
    }

    private fun setupObservers() {
        viewModel.tasks.observe(this) { tasks ->
            taskAdapter?.submitList(tasks)
        }

        viewModel.comments.observe(this) { comments ->
            commentAdapter?.updateData(comments)
            updateStatistics(comments)
        }

        viewModel.loading.observe(this) { isLoading ->
            binding.swipeRefreshLayout.isRefreshing = isLoading
        }

        viewModel.error.observe(this) { errorMsg ->
            if (errorMsg != null && errorMsg.isNotEmpty()) {
                ToastUtil.show(this, errorMsg)
            }
        }

        viewModel.taskResult.observe(this) { result ->
            result?.let {
                ToastUtil.show(this, "操作成功")
                loadTasks()
            }
        }
        
        viewModel.messageResult.observe(this) { result ->
            result?.let {
                if (it.success) {
                    ToastUtil.show(this, "消息发送成功")
                } else {
                    ToastUtil.show(this, "发送失败: ${it.errorMessage}")
                }
            }
        }
        
        viewModel.leadResult.observe(this) { result ->
            result?.let {
                ToastUtil.show(this, "商机生成成功: 共${it.generatedCount ?: 0}条")
            }
        }
    }

    private fun updateStatistics(comments: List<SocialComment>) {
        val totalCount = comments.size
        val highIntentCount = comments.count { it.isHighIntent == true }
        val withPhoneCount = comments.count { it.hasPhoneContact == true }
        with (binding) {
            tvTotalCount.text = "总数: $totalCount"
            tvHighIntentCount.text = "高意向: $highIntentCount"
            tvPhoneCount.text = "有电话: $withPhoneCount"
        }
    }

    private fun startSendMessageActivity(comment: SocialComment) {
        val intent = Intent(this, SendMessageActivity::class.java).apply {
            putExtra("comment", comment)
        }
        startActivity(intent)
    }

    private fun startBatchMessageActivity() {
        if (selectedComments.isEmpty()) {
            ToastUtil.show(this, "请先选择评论")
            return
        }
        ToastUtil.show(this, "批量消息功能开发中")
    }

    private fun startBatchGenerateLead() {
        if (selectedComments.isEmpty()) {
            ToastUtil.show(this, "请先选择评论")
            return
        }
        val ids = selectedComments.mapNotNull { it.id }.toLongArray()
        viewModel.batchGenerateLeads(ids)
    }

    private fun showGenerateLeadsDialog(task: CrawlTask) {
        GenerateLeadsDialog.newInstance(
            taskId = task.id ?: 0L,
            totalComments = task.totalCommentsFound ?: 0,
            highIntentCount = task.highIntentCount ?: 0
        ).apply {
            show(supportFragmentManager, "generate_leads")
            setOnConfirmListener { minScore, autoAssign ->
                viewModel.generateLeadsFromTask(task.id ?: 0L, minScore, autoAssign)
            }
        }
    }

    private fun loadTasks() {
        viewModel.loadTasks()
    }

    inner class CrawlPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        
        private val taskFragment = TaskListFragment()
        private val commentFragment = CommentListFragment()

        override fun getCount(): Int = 2

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> taskFragment
                else -> commentFragment
            }
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> "抓取任务"
                else -> "评论列表"
            }
        }
    }
    
    inner class TaskListFragment : Fragment() {
        
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_task_list, container, false)
        }
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view_tasks)
            recyclerView.layoutManager = LinearLayoutManager(context)
            taskAdapter?.let { recyclerView.adapter = it }
        }
    }
    
    inner class CommentListFragment : Fragment() {
        
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.fragment_comment_list, container, false)
        }
        
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recycler_view_comments)
            recyclerView.layoutManager = LinearLayoutManager(context)
            commentAdapter?.let { recyclerView.adapter = it }
        }
    }

    companion object {
        const val REQUEST_CODE_CREATE_TASK = 1001
    }
}
