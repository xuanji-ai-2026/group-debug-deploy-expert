package com.beijixing.app.ui.crawl

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.beijixing.app.R
import com.beijixing.app.data.model.CreateTaskRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class CreateTaskDialog : DialogFragment() {

    private var onTaskCreated: ((CreateTaskRequest) -> Unit)? = null

    private lateinit var spinnerPlatform: Spinner
    private lateinit var etTargetUrl: TextInputEditText
    private lateinit var etKeywords: TextInputEditText
    private lateinit var etMaxComments: TextInputEditText
    private lateinit var etDuration: TextInputEditText
    private lateinit var cbAutoAnalyze: CheckBox
    private lateinit var cbExtractPhone: CheckBox

    fun setOnTaskCreatedListener(listener: (CreateTaskRequest) -> Unit): CreateTaskDialog {
        this.onTaskCreated = listener
        return this
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
        return inflater.inflate(R.layout.dialog_create_task, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupPlatformSpinner()
        setupButtons()
    }

    private fun initViews(view: View) {
        spinnerPlatform = view.findViewById(R.id.spinner_platform)
        etTargetUrl = view.findViewById(R.id.et_target_url)
        etKeywords = view.findViewById(R.id.et_keywords)
        etMaxComments = view.findViewById(R.id.et_max_comments)
        etDuration = view.findViewById(R.id.et_duration)
        cbAutoAnalyze = view.findViewById(R.id.cb_auto_analyze)
        cbExtractPhone = view.findViewById(R.id.cb_extract_phone)
    }

    private fun setupPlatformSpinner() {
        val platforms = arrayOf("抖音", "快手", "小红书", "B站", "微博")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, platforms)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPlatform.adapter = adapter
    }

    private fun setupButtons() {
        view?.findViewById<MaterialButton>(R.id.btn_create)?.setOnClickListener {
            createTask()
        }

        view?.findViewById<MaterialButton>(R.id.btn_cancel)?.setOnClickListener {
            dismiss()
        }
    }

    private fun createTask() {
        val platform = spinnerPlatform.selectedItem.toString()
        val targetUrl = etTargetUrl.text.toString().trim()
        val keywordsStr = etKeywords.text.toString().trim()

        if (targetUrl.isEmpty() && keywordsStr.isEmpty()) {
            Toast.makeText(context, "请输入目标链接或关键词", Toast.LENGTH_SHORT).show()
            return
        }

        val keywords = if (keywordsStr.isNotEmpty()) keywordsStr.split(",").map { it.trim() } else emptyList()
        val taskName = "${platform}爬取任务-${System.currentTimeMillis()}"

        val request = CreateTaskRequest(
            name = taskName,
            type = "ACQUIRE",
            platforms = listOf(platform),
            keywords = keywords
        )

        onTaskCreated?.invoke(request)
        dismiss()
    }

    companion object {
        const val TAG = "create_task"

        fun newInstance(): CreateTaskDialog {
            return CreateTaskDialog()
        }
    }
}
