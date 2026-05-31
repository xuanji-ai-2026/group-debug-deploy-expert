package com.beijixing.app.ui.crawl

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.beijixing.app.R
import com.beijixing.app.data.model.MessageTemplate
import com.beijixing.app.data.model.SocialComment
import com.beijixing.app.databinding.ActivitySendMessageBinding
import com.beijixing.app.ui.crawl.viewmodel.CrawlViewModel
import com.beijixing.app.util.ToastUtil
import androidx.activity.viewModels
import com.bumptech.glide.Glide

class SendMessageActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendMessageBinding
    private val viewModel: CrawlViewModel by viewModels()
    
    private var comment: SocialComment? = null
    private var selectedTemplateId: Long? = null
    
    private val templateList = mutableListOf<MessageTemplate>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        loadTemplates()
        
        comment = intent.getParcelableExtra("comment")
        setupCommentInfo()
    }

    private fun initViews() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "发送私信"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSend.setOnClickListener {
            sendMessage()
        }

        binding.spinnerTemplate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0 && position <= templateList.size) {
                    selectedTemplateId = templateList[position - 1].id
                    applyTemplate(templateList[position - 1])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        binding.etContent.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateCharCount()
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupCommentInfo() {
        comment?.let { c ->
            with(binding) {
                tvAuthorName.text = c.authorName ?: "未知用户"
                
                if (c.authorAvatar != null && c.authorAvatar!!.isNotEmpty()) {
                    Glide.with(this@SendMessageActivity)
                        .load(c.authorAvatar)
                        .circleCrop()
                        .placeholder(R.drawable.ic_default_avatar)
                        .error(R.drawable.ic_default_avatar)
                        .into(ivAuthorAvatar)
                } else {
                    ivAuthorAvatar.setImageResource(R.drawable.ic_default_avatar)
                }
                
                if (c.hasPhoneContact == true || c.hasWechatContact == true) {
                    layoutContactInfo.visibility = View.VISIBLE
                    
                    if (c.extractedPhone != null && c.extractedPhone!!.isNotEmpty()) {
                        tvPhone.text = "电话: ${c.extractedPhone}"
                        tvPhone.visibility = View.VISIBLE
                    }
                    
                    if (c.extractedWechat != null && c.extractedWechat!!.isNotEmpty()) {
                        tvWechat.text = "微信: ${c.extractedWechat}"
                        tvWechat.visibility = View.VISIBLE
                    }
                    
                    tvIntentScore.text = "意向分: ${c.aiIntentScore ?: 0}"
                    
                    when (c.aiIntentLevel) {
                        "A" -> {
                            tvIntentLevel.text = "A级-超高意向"
                            tvIntentLevel.setBackgroundResource(R.drawable.bg_tag_a)
                        }
                        "B" -> {
                            tvIntentLevel.text = "B级-高意向"
                            tvIntentLevel.setBackgroundResource(R.drawable.bg_tag_b)
                        }
                        "C" -> {
                            tvIntentLevel.text = "C级-中意向"
                            tvIntentLevel.setBackgroundResource(R.drawable.bg_tag_c)
                        }
                        else -> {
                            tvIntentLevel.text = "D级-低意向"
                            tvIntentLevel.setBackgroundResource(R.drawable.bg_tag_d)
                        }
                    }
                }
                
                val commentText = c.commentText ?: ""
                tvCommentPreview.text = if (commentText.length > 100) commentText.substring(0, 100) + "..." else commentText
            }
        }
    }

    private fun loadTemplates() {
        viewModel.loadTemplates()
        
        viewModel.templates.observe(this) { templates ->
            templateList.clear()
            templateList.addAll(templates)
            
            updateTemplateSpinner(templates)
        }
    }

    private fun updateTemplateSpinner(templates: List<MessageTemplate>) {
        val items = mutableListOf("选择模板")
        templates.forEach { tpl ->
            items.add("${tpl.templateName} (${((tpl.successRate ?: 0.0) * 100).toInt()}%成功率)")
        }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerTemplate.adapter = adapter
        
        if (templates.isNotEmpty()) {
            binding.spinnerTemplate.setSelection(findBestTemplateIndex())
        }
    }

    private fun findBestTemplateIndex(): Int {
        comment?.aiIntentLevel?.let { level ->
            for ((index, tpl) in templateList.withIndex()) {
                if (tpl.intentLevel == level) {
                    return index + 1
                }
            }
        }
        
        return 0
    }

    private fun applyTemplate(template: MessageTemplate) {
        var content = template.templateContent ?: ""
        
        content = content.replace("{昵称}", comment?.authorName ?: "亲")
        content = content.replace("{产品名}", "我们的产品")
        content = content.replace("{时间}", java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA).format(java.util.Date()))
        content = content.replace("{联系方式}", "13800138000")
        
        binding.etContent.setText(content)
    }

    private fun updateCharCount() {
        val currentLength = binding.etContent.text?.length ?: 0
        binding.tvCharCount.text = "$currentLength/500"
        
        if (currentLength > 500) {
            binding.tvCharCount.setTextColor(resources.getColor(android.R.color.holo_red_dark, theme))
        } else {
            binding.tvCharCount.setTextColor(resources.getColor(android.R.color.darker_gray, theme))
        }
    }

    private fun sendMessage() {
        val content = binding.etContent.text.toString().trim()
        
        if (content.isEmpty()) {
            ToastUtil.show(this, "请输入消息内容")
            return
        }
        
        if (content.length > 500) {
            ToastUtil.show(this, "消息内容不能超过500字")
            return
        }
        
        if (comment?.id == null) {
            ToastUtil.show(this, "评论信息异常")
            return
        }
        
        binding.btnSend.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        
        viewModel.sendMessage(comment!!.id!!, selectedTemplateId, content)
        
        viewModel.messageResult.observe(this) { result ->
            result?.let {
                binding.btnSend.isEnabled = true
                binding.progressBar.visibility = View.GONE
                
                if (it.success) {
                    setResult(RESULT_OK)
                    
                    ToastUtil.show(this, "私信发送成功！")
                    
                    finish()
                } else {
                    ToastUtil.show(this, "发送失败: ${it.errorMessage}")
                }
            }
        }
    }

    companion object {
        const val EXTRA_COMMENT = "comment"
        const val RESULT_MESSAGE_SENT = 1002
    }
}
