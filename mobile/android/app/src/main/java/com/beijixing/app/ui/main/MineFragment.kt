package com.beijixing.app.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.beijixing.app.R
import com.beijixing.app.data.model.User
import com.beijixing.app.data.model.UserBalance
import com.beijixing.app.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MineFragment : Fragment() {

    companion object {
        private const val TAG = "MineFragment"
        
        fun newInstance(): MineFragment {
            return MineFragment()
        }
    }

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var tvNickname: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvRoleBadge: TextView
    private lateinit var tvPointsValue: TextView
    private lateinit var tvBalanceValue: TextView
    private lateinit var tvDeviceCount: TextView
    private lateinit var btnLogout: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mine, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initView(view)
        loadRealData()
        setupClickListeners()
    }

    private fun initView(view: View) {
        tvNickname = view.findViewById(R.id.tvNickname)
        tvPhone = view.findViewById(R.id.tvPhone)
        tvRoleBadge = view.findViewById(R.id.tvRoleBadge)
        tvPointsValue = view.findViewById(R.id.tvPointsValue)
        tvBalanceValue = view.findViewById(R.id.tvBalanceValue)
        tvDeviceCount = view.findViewById(R.id.tvDeviceCount)
        btnLogout = view.findViewById(R.id.btnLogout)
    }

    private fun loadRealData() {
        lifecycleScope.launch {
            try {
                // 加载用户信息
                val userResult = userRepository.getUserInfo()
                if (userResult.isSuccess) {
                    val user = userResult.getOrThrow()
                    updateUserInfo(user)
                }

                // 加载用户余额和积分
                val balanceResult = userRepository.getUserBalance()
                if (balanceResult.isSuccess) {
                    val balance = balanceResult.getOrThrow()
                    updateBalanceInfo(balance)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "加载用户数据失败", e)
                showError("加载失败：${e.message}")
            }
        }
    }

    private fun updateUserInfo(user: User) {
        tvNickname.text = user.nickname.ifEmpty { "未设置昵称" }
        
        // 手机号脱敏显示
        if (user.phone.length >= 11) {
            tvPhone.text = "${user.phone.substring(0, 3)}****${user.phone.substring(7)}"
        } else {
            tvPhone.text = user.phone
        }
        
        when (user.role) {
            "ADMIN" -> {
                tvRoleBadge.text = "管理员"
                tvRoleBadge.setBackgroundResource(R.drawable.bg_role_admin)
            }
            else -> {
                tvRoleBadge.text = "操作员"
                tvRoleBadge.setBackgroundResource(R.drawable.bg_role_operator)
            }
        }
        
        tvDeviceCount.text = "0/5"
    }

    private fun updateBalanceInfo(balance: UserBalance) {
        val points = balance.points ?: 0L
        tvPointsValue.text = String.format("%,d".format(java.util.Locale.CHINA, points))
        
        val amount = balance.balance ?: 0.0
        tvBalanceValue.text = "¥${String.format("%.2f", amount)}"
    }

    private fun setupClickListeners() {
        view?.findViewById<RelativeLayout>(R.id.menuAccountManagement)?.setOnClickListener {
            showToast("账号管理")
        }
        view?.findViewById<RelativeLayout>(R.id.menuSocialAccounts)?.setOnClickListener {
            showToast("社交媒体账号管理")
        }
        view?.findViewById<RelativeLayout>(R.id.menuRecharge)?.setOnClickListener {
            showToast("充值中心")
        }
        view?.findViewById<RelativeLayout>(R.id.menuMessages)?.setOnClickListener {
            showToast("消息通知（3条未读）")
        }
        view?.findViewById<RelativeLayout>(R.id.menuSecurity)?.setOnClickListener {
            showToast("安全设置")
        }
        view?.findViewById<RelativeLayout>(R.id.menuHelp)?.setOnClickListener {
            showToast("帮助与反馈")
        }
        view?.findViewById<RelativeLayout>(R.id.menuAbout)?.setOnClickListener {
            val versionName = try { com.beijixing.app.BuildConfig.VERSION_NAME } catch (e: Exception) { "1.0.24" }
            val versionCode = try { com.beijixing.app.BuildConfig.VERSION_CODE } catch (e: Exception) { 24 }
            showToast("北极星AI v$versionName (Build $versionCode)")
        }
        
        btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }
    }

    private fun showLogoutConfirmDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("确定") { _, _ ->
                lifecycleScope.launch {
                    try {
                        userRepository.logout()
                        showToast("已退出登录")
                        Log.d("MineFragment", "跳转到登录页面")
                    } catch (e: Exception) {
                        showError("退出失败：${e.message}")
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }
}
