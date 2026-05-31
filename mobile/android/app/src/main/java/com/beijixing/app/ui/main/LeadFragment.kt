package com.beijixing.app.ui.main

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beijixing.app.R
import com.beijixing.app.data.model.Lead
import com.beijixing.app.data.model.LeadStats
import com.beijixing.app.data.repository.LeadRepository
import dagger.hilt.android.AndroidEntryPoint
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LeadFragment : Fragment() {

    companion object {
        private const val TAG = "LeadFragment"
        
        fun newInstance(): LeadFragment {
            return LeadFragment()
        }
    }

    @Inject
    lateinit var leadRepository: LeadRepository

    private lateinit var tvStatAllCount: TextView
    private lateinit var tvStatNewCount: TextView
    private lateinit var tvStatFollowingCount: TextView
    private lateinit var tvStatDealedCount: TextView
    private lateinit var tvStatHighLevelCount: TextView
    private lateinit var etSearch: EditText
    private lateinit var chipGroupStatus: ChipGroup
    private lateinit var tvLeadListTitle: TextView
    private lateinit var rvLeadList: RecyclerView

    private val allLeads = mutableListOf<Lead>()
    private val filteredLeads = mutableListOf<Lead>()
    private var currentStatusFilter: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_lead, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initView(view)
        loadRealData()
        setupListeners()
    }

    private fun initView(view: View) {
        tvStatAllCount = view.findViewById(R.id.tvStatAllCount)
        tvStatNewCount = view.findViewById(R.id.tvStatNewCount)
        tvStatFollowingCount = view.findViewById(R.id.tvStatFollowingCount)
        tvStatDealedCount = view.findViewById(R.id.tvStatDealedCount)
        tvStatHighLevelCount = view.findViewById(R.id.tvStatHighLevelCount)
        etSearch = view.findViewById(R.id.etSearch)
        chipGroupStatus = view.findViewById(R.id.chipGroupStatus)
        tvLeadListTitle = view.findViewById(R.id.tvLeadListTitle)
        rvLeadList = view.findViewById(R.id.rvLeadList)

        rvLeadList.layoutManager = LinearLayoutManager(context)
    }

    private fun loadRealData() {
        lifecycleScope.launch {
            try {
                // 加载商机统计数据
                val statsResult = leadRepository.getLeadStats()
                if (statsResult.isSuccess) {
                    val stats = statsResult.getOrThrow()
                    tvStatAllCount.text = "${stats.total ?: 0}"
                    tvStatNewCount.text = "${stats.newCount ?: 0}"
                    tvStatFollowingCount.text = "${stats.followingCount ?: 0}"
                    tvStatDealedCount.text = "${stats.dealedCount ?: 0}"
                    tvStatHighLevelCount.text = "${stats.highLevelCount ?: 0}"
                }

                // 加载商机列表
                val leadsResult = leadRepository.getLeads(page = 1, size = 50)
                if (leadsResult.isSuccess) {
                    val pageResult = leadsResult.getOrThrow()
                    allLeads.clear()
                    allLeads.addAll(pageResult.records)
                    
                    filteredLeads.clear()
                    filteredLeads.addAll(allLeads)
                    updateLeadList()
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "加载商机数据失败", e)
                showError("加载失败：${e.message}")
            }
        }
    }

    private fun setupListeners() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterLeads(s?.toString() ?: "", currentStatusFilter)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        view?.findViewById<ImageButton>(R.id.btnSearch)?.setOnClickListener {
            val keyword = etSearch.text.toString()
            filterLeads(keyword, currentStatusFilter)
        }

        chipGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipAll -> currentStatusFilter = null
                R.id.chipNew -> currentStatusFilter = "NEW"
                R.id.chipFollowing -> currentStatusFilter = "FOLLOWING"
                R.id.chipDealed -> currentStatusFilter = "DEALED"
            }
            filterLeads(etSearch.text.toString(), currentStatusFilter)
        }
    }

    private fun filterLeads(keyword: String, statusFilter: String?) {
        filteredLeads.clear()
        
        filteredLeads.addAll(allLeads.filter { lead ->
            val matchesKeyword = keyword.isEmpty() ||
                    lead.name.contains(keyword, ignoreCase = true) ||
                    (lead.companyName?.contains(keyword, ignoreCase = true) ?: false) ||
                    (lead.needs?.contains(keyword, ignoreCase = true) ?: false)
            
            val matchesStatus = statusFilter == null || lead.status == statusFilter
            
            matchesKeyword && matchesStatus
        })
        
        updateLeadList()
    }

    private fun updateLeadList() {
        tvLeadListTitle.text = "商机列表 (${filteredLeads.size})"
        
        if (rvLeadList.adapter == null) {
            rvLeadList.adapter = LeadListAdapter(filteredLeads) { lead ->
                showToast("查看商机详情：${lead.name}")
            }
        } else {
            (rvLeadList.adapter as? LeadListAdapter)?.updateData(filteredLeads)
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
    }

    private inner class LeadListAdapter(
        private var leads: List<Lead>,
        private val onItemClick: (Lead) -> Unit
    ) : RecyclerView.Adapter<LeadListAdapter.LeadViewHolder>() {

        inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.leadName)
            val tvCompany: TextView = itemView.findViewById(R.id.leadCompany)
            val tvSource: TextView = itemView.findViewById(R.id.leadSource)
            val tvLevel: TextView = itemView.findViewById(R.id.leadLevel)
            val tvNeeds: TextView = itemView.findViewById(R.id.leadNeeds)
            val tvTime: TextView = itemView.findViewById(R.id.leadTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.item_lead_card, parent, false)
            return LeadViewHolder(view)
        }

        override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
            val lead = leads[position]
            holder.tvName.text = lead.name
            holder.tvCompany.text = lead.companyName ?: "个人客户"
            holder.tvSource.text = lead.getSourceName()
            holder.tvLevel.text = lead.getLevelText()
            holder.tvNeeds.text = lead.needs
            holder.tvTime.text = lead.createTime
            
            val levelColorRes = when (lead.level) {
                "HIGH" -> R.color.lead_level_high
                "MEDIUM" -> R.color.warning_color
                else -> R.color.text_secondary
            }
            holder.tvLevel.setTextColor(resources.getColor(levelColorRes, null))
            
            holder.itemView.setOnClickListener { onItemClick(lead) }
        }

        override fun getItemCount(): Int = leads.size

        fun updateData(newLeads: List<Lead>) {
            leads = newLeads
            notifyDataSetChanged()
        }
    }
}
