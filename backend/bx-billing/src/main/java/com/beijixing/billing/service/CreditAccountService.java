package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.CreditAccountDTO;
import com.beijixing.billing.entity.CreditAccount;
import com.beijixing.billing.mapper.CreditAccountMapper;
import com.beijixing.billing.util.RedisDistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("nullness")
public class CreditAccountService {
    
    private final CreditAccountMapper creditAccountMapper;
    private final StringRedisTemplate redisTemplate;
    
    @Transactional
    public CreditAccountDTO getOrCreateAccount(Long tenantId, Long userId) {
        CreditAccount account = creditAccountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CreditAccount>()
                .eq(CreditAccount::getTenantId, tenantId)
                .eq(CreditAccount::getUserId, userId)
        );
        
        if (account == null) {
            account = new CreditAccount();
            account.setTenantId(tenantId);
            account.setUserId(userId);
            account.setBalance(0L);
            account.setFrozenAmount(0L);
            account.setTotalRecharge(0L);
            account.setTotalConsumption(0L);
            account.setStatus(BillingConstants.ACCOUNT_STATUS_ACTIVE);
            creditAccountMapper.insert(account);
        }
        
        return convertToDTO(account);
    }
    
    public CreditAccountDTO getAccount(Long tenantId, Long userId) {
        CreditAccount account = creditAccountMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CreditAccount>()
                .eq(CreditAccount::getTenantId, tenantId)
                .eq(CreditAccount::getUserId, userId)
        );
        return account != null ? convertToDTO(account) : null;
    }
    
    @Transactional
    public boolean addBalance(Long accountId, Long amount) {
        String lockKey = "account:" + accountId;
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 10);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                log.error("Failed to acquire lock for account: {}", accountId);
                return false;
            }
            
            int rows = creditAccountMapper.addBalance(accountId, amount);
            return rows > 0;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public boolean deductBalance(Long accountId, Long amount) {
        String lockKey = "account:" + accountId;
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 10);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                log.error("Failed to acquire lock for account: {}", accountId);
                return false;
            }
            
            int rows = creditAccountMapper.deductBalance(accountId, amount);
            return rows > 0;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public boolean freezeAmount(Long accountId, Long amount) {
        String lockKey = "account:" + accountId;
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 10);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                return false;
            }
            
            int rows = creditAccountMapper.freezeAmount(accountId, amount);
            return rows > 0;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public boolean unfreezeAmount(Long accountId, Long amount) {
        String lockKey = "account:" + accountId;
        RedisDistributedLock lock = new RedisDistributedLock(redisTemplate, lockKey, 10);
        
        try {
            boolean acquired = lock.tryLock();
            if (!acquired) {
                return false;
            }
            
            int rows = creditAccountMapper.unfreezeAmount(accountId, amount);
            return rows > 0;
        } finally {
            lock.unlock();
        }
    }
    
    @Transactional
    public boolean freezeAccount(Long accountId) {
        CreditAccount account = creditAccountMapper.selectById(accountId);
        if (account == null) {
            return false;
        }
        account.setStatus(BillingConstants.ACCOUNT_STATUS_FROZEN);
        creditAccountMapper.updateById(account);
        return true;
    }
    
    @Transactional
    public boolean unfreezeAccount(Long accountId) {
        CreditAccount account = creditAccountMapper.selectById(accountId);
        if (account == null) {
            return false;
        }
        account.setStatus(BillingConstants.ACCOUNT_STATUS_ACTIVE);
        creditAccountMapper.updateById(account);
        return true;
    }
    
    @SuppressWarnings("nullness")
    private CreditAccountDTO convertToDTO(CreditAccount account) {
        CreditAccountDTO dto = new CreditAccountDTO();
        BeanUtils.copyProperties(account, dto);
        return dto;
    }
}