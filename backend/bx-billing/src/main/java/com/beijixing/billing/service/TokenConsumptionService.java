package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.ConsumptionRecordDTO;
import com.beijixing.billing.dto.TokenConsumptionDTO;
import com.beijixing.billing.entity.ConsumptionRecord;
import com.beijixing.billing.entity.CreditAccount;
import com.beijixing.billing.mapper.ConsumptionRecordMapper;
import com.beijixing.billing.mapper.CreditAccountMapper;
import com.beijixing.billing.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class TokenConsumptionService {
    
    private final ConsumptionRecordMapper consumptionRecordMapper;
    private final CreditAccountMapper creditAccountMapper;
    private final CreditAccountService creditAccountService;
    private final StringRedisTemplate redisTemplate;
    
    public long calculateConsumptionCost(long tokenCount, int resourceMinutes) {
        long tokenCost = tokenCount * BillingConstants.TOKEN_UNIT_PRICE;
        long resourceCost = (long) resourceMinutes * BillingConstants.RESOURCE_USAGE_FEE_PER_MIN;
        return tokenCost + resourceCost;
    }
    
    @Transactional
    public ConsumptionRecordDTO consumeToken(TokenConsumptionDTO dto) {
        long tokenCost = dto.getTokenCount() * BillingConstants.TOKEN_UNIT_PRICE;
        long resourceCost = (dto.getResourceUsageMinutes() != null ? dto.getResourceUsageMinutes() : 0) 
                           * BillingConstants.RESOURCE_USAGE_FEE_PER_MIN;
        long totalCost = tokenCost + resourceCost;
        
        CreditAccount account = creditAccountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CreditAccount>()
                .eq(CreditAccount::getTenantId, dto.getTenantId())
                .eq(CreditAccount::getUserId, dto.getUserId())
        );
        
        if (account == null) {
            log.error("Account not found for user: {}", dto.getUserId());
            throw new RuntimeException("账户不存在");
        }
        
        if (account.getBalance() < totalCost) {
            log.error("Insufficient balance for user: {}, required: {}, available: {}", 
                     dto.getUserId(), totalCost, account.getBalance());
            throw new RuntimeException("余额不足");
        }
        
        String lockKey = "account:" + account.getId();
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 10);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                throw new RuntimeException("系统繁忙，请稍后重试");
            }
            
            boolean deducted = creditAccountService.deductBalance(account.getId(), totalCost);
            if (!deducted) {
                throw new RuntimeException("扣费失败");
            }
            
            ConsumptionRecord record = new ConsumptionRecord();
            record.setTenantId(dto.getTenantId());
            record.setUserId(dto.getUserId());
            record.setCallId(dto.getCallId());
            record.setModule(dto.getModule());
            record.setTokenCount(dto.getTokenCount());
            record.setResourceUsageMinutes(dto.getResourceUsageMinutes());
            record.setTokenCost(tokenCost);
            record.setResourceCost(resourceCost);
            record.setTotalCost(totalCost);
            record.setUnitPrice(BillingConstants.TOKEN_UNIT_PRICE);
            record.setRequestSummary(dto.getRequestSummary());
            record.setResponseSummary(dto.getResponseSummary());
            record.setCallTime(java.time.LocalDateTime.now());
            
            consumptionRecordMapper.insert(record);
            
            ConsumptionRecordDTO result = new ConsumptionRecordDTO();
            BeanUtils.copyProperties(record, result);
            return result;
            
        } finally {
            lock.unlock();
        }
    }
    
    public List<ConsumptionRecordDTO> getUserConsumptionRecords(Long userId, int page, int size) {
        List<ConsumptionRecord> records = consumptionRecordMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ConsumptionRecord>()
                .eq(ConsumptionRecord::getUserId, userId)
                .orderByDesc(ConsumptionRecord::getCreateTime)
                .last("LIMIT " + (page - 1) * size + ", " + size)
        );
        return records.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public ConsumptionRecordDTO getConsumptionByCallId(String callId) {
        ConsumptionRecord record = consumptionRecordMapper.selectByCallId(callId);
        return record != null ? convertToDTO(record) : null;
    }
    
    @SuppressWarnings("nullness")
    private ConsumptionRecordDTO convertToDTO(ConsumptionRecord record) {
        ConsumptionRecordDTO dto = new ConsumptionRecordDTO();
        BeanUtils.copyProperties(record, dto);
        return dto;
    }
}