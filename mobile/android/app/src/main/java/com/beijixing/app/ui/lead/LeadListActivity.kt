package com.beijixing.app.ui.lead

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
import com.beijixing.app.data.model.Lead
import com.beijixing.app.ui.components.LeadAdapter
import com.beijixing.app.util.GlobalExceptionHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LeadListActivity : AppCompatActivity() {

    private val viewModel: LeadViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LeadAdapter
    private lateinit var progressBar: View
    private lateinit var emptyView: View
    private lateinit var etSearch: EditText
    
    companion object {
        const val EXTRA_LEAD_ID = "lead_id"
        private const val TAG = "LeadListActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lead_list)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        observeState()
        
        viewModel.loadLeads(1)
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView) ?: RecyclerView(this)
        progressBar = findViewById(R.id.progressBar) ?: View(this)
        emptyView = findViewById(R.id.emptyView) ?: View(this)
        etSearch = findViewById(R.id.etSearch) ?: EditText(this).apply { hint = "搜索商机" }
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "商机管理"
            it.setNavigationOnClickListener { finish() }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = LeadAdapter(
            onItemClick = { item ->
                if (item is Lead) {
                    navigateToDetail(item.id)
                }
            },
            onItemDelete = { item ->
                if (item is Lead) {
                    showDeleteConfirmDialog(item)
                }
            }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LeadListActivity)
            adapter = this@LeadListActivity.adapter
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
    
    private fun updateUI(state: LeadUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        if (state.leads.isNotEmpty()) {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(state.leads)
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

    private fun navigateToDetail(leadId: Long) {
        try {
            Intent(this, Class.forName("com.beijixing.app.ui.lead.LeadDetailActivity")).apply {
                putExtra(EXTRA_LEAD_ID, leadId)
                startActivity(this)
            }
        } catch (e: Exception) {
            GlobalExceptionHandler.handle(e, this)
        }
    }
    
    private fun showDeleteConfirmDialog(lead: Lead) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除商机「${lead.companyName ?: lead.name}」吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteLead(lead.id) }
            .setNegativeButton("取消", null)
            .show()
    }
}
