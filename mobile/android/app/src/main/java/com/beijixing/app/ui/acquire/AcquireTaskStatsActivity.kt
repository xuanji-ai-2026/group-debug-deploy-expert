package com.beijixing.app.ui.acquire

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R

class AcquireTaskStatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acquire_task_stats)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "获客任务统计"
        }
    }
}
