package com.beijixing.risk.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.risk.entity.RiskScore;
import com.beijixing.risk.repository.mapper.RiskScoreMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * 风控评分 Repository
 *
 * @author 林超 (EMP-SEC-001)
 */
@Repository
public class RiskScoreRepository extends ServiceImpl<RiskScoreMapper, RiskScore> {

    /**
     * 根据对象类型和ID获取最新评分
     */
    public RiskScore getByObject(String objectType, String objectId) {
        LambdaQueryWrapper<RiskScore> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RiskScore::getObjectType, objectType)
               .eq(RiskScore::getObjectId, objectId)
               .orderByDesc(RiskScore::getScoreTime)
               .last("LIMIT 1");
        return this.getOne(wrapper);
    }

    /**
     * 获取账号评分
     */
    public RiskScore getAccountScore(Long accountId) {
        return getByObject("account", accountId.toString());
    }

    /**
     * 更新或创建评分
     */
    public void upsertScore(String objectType, String objectId, Long tenantId,
                           int totalScore, String riskLevel) {
        RiskScore existing = getByObject(objectType, objectId);
        if (existing != null) {
            existing.setTotalScore(totalScore);
            existing.setRiskLevel(riskLevel);
            existing.setScoreTime(LocalDateTime.now());
            existing.setUpdateTime(LocalDateTime.now());
            this.updateById(existing);
        } else {
            RiskScore newScore = new RiskScore();
            newScore.setObjectType(objectType);
            newScore.setObjectId(objectId);
            newScore.setTenantId(tenantId);
            newScore.setTotalScore(totalScore);
            newScore.setRiskLevel(riskLevel);
            newScore.setScoreTime(LocalDateTime.now());
            newScore.setCreateTime(LocalDateTime.now());
            newScore.setUpdateTime(LocalDateTime.now());
            this.save(newScore);
        }
    }
}
