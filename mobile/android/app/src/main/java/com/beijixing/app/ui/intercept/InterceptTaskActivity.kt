package com.beijixing.app.ui.intercept

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R

class InterceptTaskActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intercept_task)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "同业截客任务"
        }
    }
}
