package com.beijixing.app.ui.acquire

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R

class AcquireTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acquire_task)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "主动获客任务"
        }
    }
}
