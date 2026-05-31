package com.beijixing.app.ui.account

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
import com.beijixing.app.data.model.SocialAccount
import com.beijixing.app.util.SocialMediaHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountActivity : AppCompatActivity() {

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AccountAdapter
    private lateinit var progressBar: View
    private lateinit var emptyView: View
    private lateinit var fabAdd: com.google.android.material.floatingactionbutton.FloatingActionButton
    
    companion object {
        const val EXTRA_ACCOUNT_ID = "account_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)
        
        initViews()
        setupToolbar()
        setupRecyclerView()
        observeState()
        
        viewModel.loadAccounts()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView) ?: RecyclerView(this)
        progressBar = findViewById(R.id.progressBar) ?: View(this)
        emptyView = findViewById(R.id.emptyView) ?: View(this)
        fabAdd = findViewById(R.id.fabAddAccount) ?: com.google.android.material.floatingactionbutton.FloatingActionButton(this)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "账号管理"
            it.setNavigationOnClickListener { finish() }
        }
    }
    
    private fun setupRecyclerView() {
        adapter = AccountAdapter(
            onItemClick = { account -> navigateToDetail((account as? SocialAccount)?.id ?: 0L) },
            onItemDelete = { account -> showDeleteConfirmDialog(account as? SocialAccount ?: return@AccountAdapter) }
        )
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@AccountActivity)
            adapter = this@AccountActivity.adapter
        }
        
        fabAdd.setOnClickListener { showAddDialog() }
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
    
    private fun updateUI(state: AccountUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        if (state.accounts.isNotEmpty()) {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            adapter.submitList(state.accounts)
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

    private fun navigateToDetail(accountId: Long) {
        val account = adapter.getCurrentList().find { (it as? SocialAccount)?.id == accountId } as? SocialAccount ?: return
        
        if (!SocialMediaHelper.isPlatformInstalled(this, account.platform)) {
            SocialMediaHelper.showInstallDialog(this,
                SocialMediaHelper.SUPPORTED_PLATFORMS.find { it.name == account.platform }
                    ?: return
            )
        } else {
            val success = SocialMediaHelper.openUserProfile(this, account.platform, account.accountNo)
            if (!success) {
                Toast.makeText(this, "无法打开${account.platform}，请检查是否已安装", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun showAddDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        
        val etPlatform = EditText(this).apply { hint = "平台（微信/抖音/小红书）" }
        val etName = EditText(this).apply { hint = "账号名称" }
        val etAccountNo = EditText(this).apply { hint = "账号ID/手机号" }
        
        layout.addView(etPlatform)
        layout.addView(etName)
        layout.addView(etAccountNo)
        
        AlertDialog.Builder(this)
            .setTitle("添加社交媒体账号")
            .setView(layout)
            .setPositiveButton("添加") { _, _ ->
                val platform = etPlatform.text.toString().trim()
                val name = etName.text.toString().trim()
                val accountNo = etAccountNo.text.toString().trim()
                
                if (platform.isNotEmpty() && name.isNotEmpty() && accountNo.isNotEmpty()) {
                    viewModel.addAccount(platform, name, accountNo)
                } else {
                    Toast.makeText(this, "请填写完整信息", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showDeleteConfirmDialog(account: SocialAccount) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除账号「${account.accountName}」吗？")
            .setPositiveButton("删除") { _, _ -> viewModel.deleteAccount(account.id) }
            .setNegativeButton("取消", null)
            .show()
    }
}
