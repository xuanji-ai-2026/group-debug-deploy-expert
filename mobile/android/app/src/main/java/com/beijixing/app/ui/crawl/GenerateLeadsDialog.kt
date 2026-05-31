package com.beijixing.app.ui.crawl

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.beijixing.app.R

class GenerateLeadsDialog : DialogFragment() {

    private var taskId: Long = 0L
    private var totalComments: Int = 0
    private var highIntentCount: Int = 0
    private var onConfirmListener: ((Int, Boolean) -> Unit)? = null
    
    private lateinit var etMinScore: EditText
    private lateinit var tvStatistics: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
        
        arguments?.let {
            taskId = it.getLong("task_id", 0L)
            totalComments = it.getInt("total_comments", 0)
            highIntentCount = it.getInt("high_intent_count", 0)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_generate_leads, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        etMinScore = view.findViewById(R.id.et_min_score)
        tvStatistics = view.findViewById(R.id.tv_statistics)
        btnConfirm = view.findViewById(R.id.btn_confirm)
        btnCancel = view.findViewById(R.id.btn_cancel)
        
        tvStatistics.text = "总评论数: $totalComments | 高意向评论: $highIntentCount"
        etMinScore.setText("60")
        
        btnConfirm.setOnClickListener {
            val minScore = etMinScore.text.toString().toIntOrNull() ?: 60
            val autoAssign = true
            
            onConfirmListener?.invoke(minScore, autoAssign)
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    fun setOnConfirmListener(listener: (Int, Boolean) -> Unit): GenerateLeadsDialog {
        this.onConfirmListener = listener
        return this
    }

    companion object {
        fun newInstance(taskId: Long, totalComments: Int, highIntentCount: Int): GenerateLeadsDialog {
            return GenerateLeadsDialog().apply {
                arguments = Bundle().apply {
                    putLong("task_id", taskId)
                    putInt("total_comments", totalComments)
                    putInt("high_intent_count", highIntentCount)
                }
            }
        }
    }
}
