package com.beijixing.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.SocialComment
import com.bumptech.glide.Glide

class CommentAdapter(
    private var comments: List<SocialComment> = emptyList(),
    private val onItemClickListener: OnItemClickListener? = null,
    private val onSendMessageClickListener: OnSendMessageClickListener? = null
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(comment: SocialComment, position: Int)
    }

    interface OnSendMessageClickListener {
        fun onSendMessageClick(comment: SocialComment, position: Int)
    }

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.iv_comment_avatar)
        val tvAuthorName: TextView = itemView.findViewById(R.id.tv_comment_author_name)
        val tvCommentText: TextView = itemView.findViewById(R.id.tv_comment_text)
        val tvIntentScore: TextView = itemView.findViewById(R.id.tv_intent_score)
        val tvIntentLevel: TextView = itemView.findViewById(R.id.tv_intent_level)
        val tvLikeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        val tvPlatform: TextView = itemView.findViewById(R.id.tv_platform)
        val tvContactInfo: TextView = itemView.findViewById(R.id.tv_contact_info)
        val btnSendMessage: View = itemView.findViewById(R.id.btn_send_message)
        val viewHighIntent: View = itemView.findViewById(R.id.view_high_intent_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        val context = holder.itemView.context

        Glide.with(context)
            .load(comment.authorAvatar)
            .placeholder(R.drawable.ic_default_avatar)
            .error(R.drawable.ic_default_avatar)
            .circleCrop()
            .into(holder.ivAvatar)

        holder.tvAuthorName.text = comment.authorName ?: "匿名用户"
        holder.tvCommentText.text = comment.commentText ?: ""
        
        holder.tvIntentScore.text = "意向分: ${comment.aiIntentScore ?: 0}"
        holder.tvIntentLevel.text = comment.aiIntentLevel ?: "未分析"
        holder.tvLikeCount.text = "${comment.likeCount ?: 0} 赞"
        holder.tvPlatform.text = getPlatformDisplayName(comment.platformCode)

        when (comment.aiIntentLevel) {
            "A" -> {
                holder.tvIntentLevel.setBackgroundResource(R.drawable.bg_level_a)
                holder.viewHighIntent.visibility = View.VISIBLE
            }
            "B" -> {
                holder.tvIntentLevel.setBackgroundResource(R.drawable.bg_level_b)
                holder.viewHighIntent.visibility = View.VISIBLE
            }
            "C" -> {
                holder.tvIntentLevel.setBackgroundResource(R.drawable.bg_level_c)
                holder.viewHighIntent.visibility = View.GONE
            }
            else -> {
                holder.tvIntentLevel.setBackgroundResource(R.drawable.bg_level_default)
                holder.viewHighIntent.visibility = View.GONE
            }
        }

        val contactInfo = StringBuilder()
        if (comment.hasPhoneContact == true && comment.extractedPhone != null) {
            contactInfo.append("📱 ${comment.extractedPhone}")
        }
        if (comment.hasWechatContact == true && comment.extractedWechat != null) {
            if (contactInfo.isNotEmpty()) contactInfo.append(" ")
            contactInfo.append("💬 ${comment.extractedWechat}")
        }
        holder.tvContactInfo.text = contactInfo.toString()
        holder.tvContactInfo.visibility = if (contactInfo.isNotEmpty()) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onItemClickListener?.onItemClick(comment, position)
        }

        holder.btnSendMessage.setOnClickListener {
            onSendMessageClickListener?.onSendMessageClick(comment, position)
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(newComments: List<SocialComment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    fun getData(): List<SocialComment> = comments

    private fun getPlatformDisplayName(platformCode: String?): String {
        return when (platformCode?.uppercase()) {
            "DOUYIN" -> "抖音"
            "XIAOHONGSHU" -> "小红书"
            "KUAISHOU" -> "快手"
            "WEIBO" -> "微博"
            "BILIBILI" -> "B站"
            else -> platformCode ?: "未知"
        }
    }
}
