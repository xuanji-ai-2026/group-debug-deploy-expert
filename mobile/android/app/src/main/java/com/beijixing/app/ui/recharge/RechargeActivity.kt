package com.beijixing.app.ui.recharge

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RechargeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recharge)

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "积分充值"
        }
        toolbar?.setNavigationOnClickListener { finish() }
        
        Snackbar.make(toolbar ?: findViewById(android.R.id.content), "充值功能开发中", Snackbar.LENGTH_LONG).show()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, RechargeActivity::class.java))
        }
    }
}
