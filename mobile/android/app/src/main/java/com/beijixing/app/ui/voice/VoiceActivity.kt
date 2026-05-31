package com.beijixing.app.ui.voice

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R

class VoiceActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)
        
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "语音输入"
        }
    }
}
