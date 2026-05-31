package com.beijixing.app.ui.lead

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R

class LeadDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lead_detail)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "商机详情"
        }
    }
}
