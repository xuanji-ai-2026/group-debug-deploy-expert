package com.beijixing.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beijixing.app.R
import com.beijixing.app.data.model.User
import com.beijixing.app.data.model.UserBalance
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()
    
    private lateinit var tvNickname: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvRole: Chip
    private lateinit var tvPoints: TextView
    private lateinit var ivAvatar: ImageView
    private lateinit var progressBar: View
    private lateinit var layoutEditProfile: View
    private lateinit var layoutChangePassword: View
    private lateinit var layoutLogout: View
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
            uri?.let { imageUri ->
                ivAvatar.setImageURI(imageUri)
                Toast.makeText(this, "头像选择成功", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        
        initViews()
        setupToolbar()
        observeState()
        
        viewModel.loadUserInfo()
        viewModel.loadBalance()
    }
    
    private fun initViews() {
        tvNickname = findViewById(R.id.tvNickname) ?: TextView(this).apply { text = "加载中..." }
        tvPhone = findViewById(R.id.tvPhone) ?: TextView(this)
        tvRole = findViewById(R.id.tvRole) ?: Chip(this).apply { text = "操作员" }
        tvPoints = findViewById(R.id.tvPoints) ?: TextView(this).apply { text = "0 积分" }
        ivAvatar = findViewById(R.id.ivAvatar) ?: ImageView(this)
        progressBar = findViewById(R.id.progressBar) ?: View(this)
        layoutEditProfile = findViewById(R.id.layoutEditProfile) ?: View(this)
        layoutChangePassword = findViewById(R.id.layoutChangePassword) ?: View(this)
        layoutLogout = findViewById(R.id.layoutLogout) ?: View(this)
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        toolbar?.let {
            setSupportActionBar(it)
            supportActionBar?.title = "个人中心"
            it.setNavigationOnClickListener { finish() }
        }
        
        layoutEditProfile.setOnClickListener { showEditDialog() }
        layoutChangePassword.setOnClickListener { showChangePasswordDialog() }
        layoutLogout.setOnClickListener { showLogoutConfirmDialog() }
        ivAvatar.setOnClickListener { pickAvatar() }
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
    
    private fun updateUI(state: ProfileUiState) {
        progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        state.user?.let { user ->
            tvNickname.text = user.nickname.ifEmpty { "未设置昵称" }
            tvPhone.text = user.phone.ifEmpty { "未绑定手机" }
            
            // 设置角色标签
            when (user.role) {
                "ADMIN", "SUPER_ADMIN" -> {
                    tvRole.text = "管理员"
                    tvRole.setChipBackgroundColorResource(R.color.error)
                    tvRole.setTextColor(resources.getColor(R.color.white, null))
                }
                "OPERATOR" -> {
                    tvRole.text = "操作员"
                    tvRole.setChipBackgroundColorResource(R.color.brand_primary_alpha_20)
                    tvRole.setTextColor(resources.getColor(R.color.brand_primary, null))
                }
                else -> {
                    tvRole.text = "用户"
                    tvRole.setChipBackgroundColorResource(R.color.bg_divider)
                    tvRole.setTextColor(resources.getColor(R.color.text_secondary, null))
                }
            }
            
            if (user.avatar?.isNotEmpty() == true) {
                try {
                    Glide.with(this)
                        .load(user.avatar)
                        .circleCrop()
                        .placeholder(R.mipmap.ic_launcher_round)
                        .into(ivAvatar)
                } catch (e: Exception) {
                    ivAvatar.setImageResource(R.mipmap.ic_launcher_round)
                }
            }
        }
        
        state.balance?.let { balance ->
            tvPoints.text = "${balance.points} 积分"
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

    private fun showEditDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        
        val etNickname = EditText(this).apply {
            hint = "昵称"
            setText(tvNickname.text.toString().takeIf { it != "加载中..." && it != "未设置昵称" })
        }
        
        layout.addView(etNickname)
        
        AlertDialog.Builder(this)
            .setTitle("编辑个人资料")
            .setView(layout)
            .setPositiveButton("保存") { _, _ ->
                val nickname = etNickname.text.toString().trim()
                
                if (nickname.isNotEmpty()) {
                    viewModel.updateProfile(nickname)
                } else {
                    Toast.makeText(this, "请输入昵称", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showChangePasswordDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 30, 50, 30)
        }
        
        val etOldPwd = EditText(this).apply { 
            hint = "当前密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                     android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etNewPwd = EditText(this).apply { 
            hint = "新密码（至少6位）"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                     android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val etConfirmPwd = EditText(this).apply { 
            hint = "确认新密码"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or 
                     android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        
        layout.addView(etOldPwd)
        layout.addView(etNewPwd)
        layout.addView(etConfirmPwd)
        
        AlertDialog.Builder(this)
            .setTitle("修改密码")
            .setView(layout)
            .setPositiveButton("修改") { _, _ ->
                val oldPwd = etOldPwd.text.toString()
                val newPwd = etNewPwd.text.toString()
                val confirmPwd = etConfirmPwd.text.toString()
                
                when {
                    oldPwd.isEmpty() -> Toast.makeText(this, "请输入当前密码", Toast.LENGTH_SHORT).show()
                    newPwd.length < 6 -> Toast.makeText(this, "新密码至少6位", Toast.LENGTH_SHORT).show()
                    newPwd != confirmPwd -> Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
                    else -> viewModel.changePassword(oldPwd, newPwd)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("确认退出")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出") { _, _ -> 
                viewModel.logout()
                finish()
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun pickAvatar() {
        pickImageLauncher.launch("image/*")
    }
    
    override fun onDestroy() {
        super.onDestroy()
    }
}
