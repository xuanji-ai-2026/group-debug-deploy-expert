package com.beijixing.risk.engine;

import com.beijixing.risk.dto.RiskCheckRequest;
import com.beijixing.risk.vo.RiskDecisionVO;

/**
 * 决策引擎接口 - 负责综合规则引擎、评分引擎和策略引擎的结果，做出最终决策
 *
 * @author 林超 (EMP-SEC-001)
 */
public interface DecisionEngine {

    /**
     * 执行完整的风控决策流程
     *
     * 流程：
     * 1. 调用规则引擎检查规则
     * 2. 调用评分引擎计算评分
     * 3. 调用策略引擎匹配策略
     * 4. 综合所有结果做出最终决策
     *
     * @param request 风控检查请求
     * @return 风控决策结果
     */
    RiskDecisionVO makeDecision(RiskCheckRequest request);

    /**
     * 快速检查（仅规则检查，不评分）
     *
     * @param request 风控检查请求
     * @return 快速检查结果
     */
    RiskDecisionVO quickCheck(RiskCheckRequest request);

    /**
     * 获取当前决策的时间戳
     */
    long getDecisionTimestamp();

    /**
     * 判断是否需要记录到风控日志
     */
    boolean needsLogging(RiskCheckRequest request);
}
