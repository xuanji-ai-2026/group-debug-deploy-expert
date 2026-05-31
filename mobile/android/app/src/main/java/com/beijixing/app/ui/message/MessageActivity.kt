package com.beijixing.app.ui.message

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.Message
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MessageActivity : AppCompatActivity() {

    private val viewModel: MessageViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: MessageAdapter
    private lateinit var progressBar: View
    private lateinit var emptyView: View
    
    companion object {
        const val EXTRA_MESSAGE_ID = "message_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        observeState()
        
        viewModel.loadMessages()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView) ?: RecyclerView(this)
        progressBar = findViewById(R.id.progressBar) ?: View(this)
        emptyView = findViewById(R.id.emptyView) ?: View(this)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "消息中心"
            it.setNavigationOnClickListener { finish() }
            
            it.inflateMenu(R.menu.menu_message)
            it.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_clear_all -> showMarkAllConfirmDialog()
                    R.id.action_refresh -> viewModel.refresh()
                }
                true
            }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = MessageAdapter(
            onItemClick = { message -> navigateToDetail((message as? Message)?.messageId ?: 0L) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MessageActivity)
            adapter = this@MessageActivity.adapter
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
    
    private fun updateUI(state: MessageUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        if (state.messages.isNotEmpty()) {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(state.messages)
            
            supportActionBar?.subtitle = "共${state.messages.size}条消息 (${state.unreadCount}条未读)"
        } else if (!state.isLoading) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }
        
        state.errorMsg?.let { msg ->
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
        
        state.successMsg?.let { msg ->
            android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
        }
    }

    private fun navigateToDetail(messageId: Long) {
        android.widget.Toast.makeText(this, "消息详情: $messageId", android.widget.Toast.LENGTH_SHORT).show()
    }
    
    private fun showMarkAllConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("全部标记已读")
            .setMessage("确定要将所有消息标记为已读吗？")
            .setPositiveButton("确定") { _, _ -> viewModel.markAllAsRead() }
            .setNegativeButton("取消", null)
            .show()
    }
}
