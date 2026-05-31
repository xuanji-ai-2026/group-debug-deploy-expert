package com.beijixing.app.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beijixing.app.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    
    private lateinit var bottomNav: BottomNavigationView
    
    private val homeFragment = HomeFragment.newInstance()
    private val leadFragment = LeadFragment.newInstance()
    private val taskFragment = TaskFragment.newInstance()
    private val mineFragment = MineFragment.newInstance()
    
    private var activeFragment: Fragment = homeFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupBottomNavigation()
        loadFragment(homeFragment)
        observeState()
    }

    private fun initViews() {
        bottomNav = findViewById(R.id.bottomNav)
    }
    
    private fun setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    switchFragment(homeFragment)
                    true
                }
                R.id.nav_lead -> {
                    switchFragment(leadFragment)
                    true
                }
                R.id.nav_task -> {
                    switchFragment(taskFragment)
                    true
                }
                R.id.nav_mine -> {
                    switchFragment(mineFragment)
                    true
                }
                else -> false
            }
        }
    }
    
    private fun switchFragment(fragment: Fragment) {
        if (activeFragment === fragment) return
        
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(fragment)
            .commit()
        
        activeFragment = fragment
    }
    
    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragmentContainer, homeFragment, "home")
            add(R.id.fragmentContainer, leadFragment, "lead")
            hide(leadFragment)
            add(R.id.fragmentContainer, taskFragment, "task")
            hide(taskFragment)
            add(R.id.fragmentContainer, mineFragment, "mine")
            hide(mineFragment)
            commit()
        }
        
        activeFragment = fragment
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 简化实现：只显示基本状态
                }
            }
        }
    }
}
