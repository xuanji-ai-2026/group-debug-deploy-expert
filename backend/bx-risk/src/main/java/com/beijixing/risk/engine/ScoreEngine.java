package com.beijixing.risk.engine;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.entity.RiskScore;

import java.util.Map;

/**
 * 评分引擎接口 - 负责计算账号/设备/IP的风险评分
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface ScoreEngine {

    /**
     * 计算综合风险评分
     *
     * @param request 风控检查请求
     * @return 评分计算结果
     */
    ScoreResult calculateScore(RiskCheckRequest request);

    /**
     * 获取账号的综合评分
     *
     * @param accountId 账号ID
     * @return 风控评分实体
     */
    RiskScore getAccountScore(Long accountId);

    /**
     * 更新账号评分
     *
     * @param accountId 账号ID
     * @param score      新评分
     */
    void updateAccountScore(Long accountId, int score);

    /**
     * 计算子维度评分
     *
     * @param request 风控检查请求
     * @return 各维度评分Map
     */
    Map<String, Integer> calculateSubScores(RiskCheckRequest request);

    /**
     * 评分结果内部类
     */
    class ScoreResult {
        /**
         * 综合评分（0-100）
         */
        private int totalScore;
        /**
         * 操作频率评分
         */
        private int frequencyScore;
        /**
         * 内容合规评分
         */
        private int complianceScore;
        /**
         * 触达成功评分
         */
        private int touchScore;
        /**
         * 账号活跃评分
         */
        private int activityScore;
        /**
         * 设备指纹评分
         */
        private int deviceScore;
        /**
         * IP评分
         */
        private int ipScore;
        /**
         * 子维度评分详情
         */
        private Map<String, Integer> subScores;

        public int getTotalScore() {
            return totalScore;
        }

        public void setTotalScore(int totalScore) {
            this.totalScore = totalScore;
        }

        public int getFrequencyScore() {
            return frequencyScore;
        }

        public void setFrequencyScore(int frequencyScore) {
            this.frequencyScore = frequencyScore;
        }

        public int getComplianceScore() {
            return complianceScore;
        }

        public void setComplianceScore(int complianceScore) {
            this.complianceScore = complianceScore;
        }

        public int getTouchScore() {
            return touchScore;
        }

        public void setTouchScore(int touchScore) {
            this.touchScore = touchScore;
        }

        public int getActivityScore() {
            return activityScore;
        }

        public void setActivityScore(int activityScore) {
            this.activityScore = activityScore;
        }

        public int getDeviceScore() {
            return deviceScore;
        }

        public void setDeviceScore(int deviceScore) {
            this.deviceScore = deviceScore;
        }

        public int getIpScore() {
            return ipScore;
        }

        public void setIpScore(int ipScore) {
            this.ipScore = ipScore;
        }

        public Map<String, Integer> getSubScores() {
            return subScores;
        }

        public void setSubScores(Map<String, Integer> subScores) {
            this.subScores = subScores;
        }
    }
}
