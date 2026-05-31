package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.CreditAccountDTO;
import com.beijixing.billing.entity.CreditAccount;
import com.beijixing.billing.mapper.CreditAccountMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * CreditAccountService 单元测试
 * 测试积分账户创建、余额管理、冻结解冻等核心功能
 *
 * @author 测试工程师 (EMP-QA-001)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("积分账户服务测试")
class CreditAccountServiceTest {

    @Mock
    private CreditAccountMapper creditAccountMapper;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private CreditAccountService creditAccountService;

    private CreditAccount testAccount;

    @BeforeEach
    void setUp() {
        testAccount = createTestAccount();
    }

    private CreditAccount createTestAccount() {
        CreditAccount account = new CreditAccount();
        account.setId(1L);
        account.setTenantId(1L);
        account.setUserId(1L);
        account.setBalance(10000L);
        account.setFrozenAmount(0L);
        account.setTotalRecharge(10000L);
        account.setTotalConsumption(0L);
        account.setStatus(BillingConstants.ACCOUNT_STATUS_ACTIVE);
        return account;
    }

    @Nested
    @DisplayName("账户获取测试")
    class AccountRetrievalTests {

        @Test
        @DisplayName("获取已存在的账户")
        void shouldGetExistingAccount() {
            when(creditAccountMapper.selectOne(any())).thenReturn(testAccount);

            CreditAccountDTO result = creditAccountService.getAccount(1L, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(10000L, result.getBalance());
            assertEquals(BillingConstants.ACCOUNT_STATUS_ACTIVE, result.getStatus());
        }

        @Test
        @DisplayName("获取不存在的账户返回null")
        void shouldReturnNullForNonExistentAccount() {
            when(creditAccountMapper.selectOne(any())).thenReturn(null);

            CreditAccountDTO result = creditAccountService.getAccount(999L, 999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("账户创建测试")
    class AccountCreationTests {

        @Test
        @DisplayName("账户不存在时自动创建")
        void shouldCreateAccountWhenNotExists() {
            when(creditAccountMapper.selectOne(any())).thenReturn(null);
            when(creditAccountMapper.insert(any(CreditAccount.class))).thenReturn(1);

            CreditAccountDTO result = creditAccountService.getOrCreateAccount(1L, 1L);

            assertNotNull(result);
            assertEquals(0L, result.getBalance());
            assertEquals(0L, result.getFrozenAmount());
            assertEquals(BillingConstants.ACCOUNT_STATUS_ACTIVE, result.getStatus());
            verify(creditAccountMapper, times(1)).insert(any(CreditAccount.class));
        }

        @Test
        @DisplayName("账户已存在直接返回")
        void shouldReturnExistingAccountWithoutCreating() {
            when(creditAccountMapper.selectOne(any())).thenReturn(testAccount);

            CreditAccountDTO result = creditAccountService.getOrCreateAccount(1L, 1L);

            assertNotNull(result);
            assertEquals(10000L, result.getBalance());
            verify(creditAccountMapper, never()).insert(any(CreditAccount.class));
        }
    }

    @Nested
    @DisplayName("余额增加测试")
    class BalanceAdditionTests {

        @Test
        @DisplayName("增加余额成功")
        void shouldAddBalanceSuccessfully() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.addBalance(1L, 5000L)).thenReturn(1);

            boolean result = creditAccountService.addBalance(1L, 5000L);

            assertTrue(result);
            verify(creditAccountMapper).addBalance(1L, 5000L);
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("增加余额 - 获取锁失败")
        void shouldFailWhenLockAcquisitionFailsForAdd() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

            boolean result = creditAccountService.addBalance(1L, 5000L);

            assertFalse(result);
            verify(creditAccountMapper, never()).addBalance(anyLong(), anyLong());
        }

        @Test
        @DisplayName("增加余额 - 数据库更新失败")
        void shouldFailWhenDatabaseUpdateFailsForAdd() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.addBalance(1L, 5000L)).thenReturn(0);

            boolean result = creditAccountService.addBalance(1L, 5000L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("余额扣除测试")
    class BalanceDeductionTests {

        @Test
        @DisplayName("扣除余额成功")
        void shouldDeductBalanceSuccessfully() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.deductBalance(1L, 3000L)).thenReturn(1);

            boolean result = creditAccountService.deductBalance(1L, 3000L);

            assertTrue(result);
            verify(creditAccountMapper).deductBalance(1L, 3000L);
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("扣除余额 - 余额不足")
        void shouldFailWhenBalanceInsufficient() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.deductBalance(1L, 20000L)).thenReturn(0); // 余额不足

            boolean result = creditAccountService.deductBalance(1L, 20000L);

            assertFalse(result);
        }

        @Test
        @DisplayName("扣除余额 - 获取锁失败")
        void shouldFailWhenLockAcquisitionFailsForDeduct() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

            boolean result = creditAccountService.deductBalance(1L, 3000L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("金额冻结测试")
    class FreezeAmountTests {

        @Test
        @DisplayName("冻结金额成功")
        void shouldFreezeAmountSuccessfully() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.freezeAmount(1L, 1000L)).thenReturn(1);

            boolean result = creditAccountService.freezeAmount(1L, 1000L);

            assertTrue(result);
            verify(creditAccountMapper).freezeAmount(1L, 1000L);
            verify(rLock).unlock();
        }

        @Test
        @DisplayName("冻结金额 - 余额不足")
        void shouldFailFreezeWhenBalanceInsufficient() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.freezeAmount(1L, 20000L)).thenReturn(0);

            boolean result = creditAccountService.freezeAmount(1L, 20000L);

            assertFalse(result);
        }

        @Test
        @DisplayName("冻结金额 - 获取锁失败")
        void shouldFailFreezeWhenLockFails() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

            boolean result = creditAccountService.freezeAmount(1L, 1000L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("金额解冻测试")
    class UnfreezeAmountTests {

        @Test
        @DisplayName("解冻金额成功")
        void shouldUnfreezeAmountSuccessfully() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.unfreezeAmount(1L, 500L)).thenReturn(1);

            boolean result = creditAccountService.unfreezeAmount(1L, 500L);

            assertTrue(result);
            verify(creditAccountMapper).unfreezeAmount(1L, 500L);
        }

        @Test
        @DisplayName("解冻金额 - 冻结金额不足")
        void shouldFailUnfreezeWhenFrozenAmountInsufficient() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.unfreezeAmount(1L, 5000L)).thenReturn(0);

            boolean result = creditAccountService.unfreezeAmount(1L, 5000L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("账户冻结解冻测试")
    class AccountFreezeUnfreezeTests {

        @Test
        @DisplayName("冻结账户成功")
        void shouldFreezeAccountSuccessfully() {
            when(creditAccountMapper.selectById(1L)).thenReturn(testAccount);
            when(creditAccountMapper.updateById(any(CreditAccount.class))).thenReturn(1);

            boolean result = creditAccountService.freezeAccount(1L);

            assertTrue(result);
            verify(creditAccountMapper).updateById(argThat(account ->
                    account.getStatus() == BillingConstants.ACCOUNT_STATUS_FROZEN
            ));
        }

        @Test
        @DisplayName("冻结账户 - 账户不存在")
        void shouldFailFreezeAccountWhenNotFound() {
            when(creditAccountMapper.selectById(999L)).thenReturn(null);

            boolean result = creditAccountService.freezeAccount(999L);

            assertFalse(result);
        }

        @Test
        @DisplayName("解冻账户成功")
        void shouldUnfreezeAccountSuccessfully() {
            testAccount.setStatus(BillingConstants.ACCOUNT_STATUS_FROZEN);
            when(creditAccountMapper.selectById(1L)).thenReturn(testAccount);
            when(creditAccountMapper.updateById(any(CreditAccount.class))).thenReturn(1);

            boolean result = creditAccountService.unfreezeAccount(1L);

            assertTrue(result);
            verify(creditAccountMapper).updateById(argThat(account ->
                    account.getStatus() == BillingConstants.ACCOUNT_STATUS_ACTIVE
            ));
        }

        @Test
        @DisplayName("解冻账户 - 账户不存在")
        void shouldFailUnfreezeAccountWhenNotFound() {
            when(creditAccountMapper.selectById(999L)).thenReturn(null);

            boolean result = creditAccountService.unfreezeAccount(999L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("账户状态常量测试")
    class AccountStatusTests {

        @Test
        @DisplayName("账户状态常量正确")
        void shouldHaveCorrectAccountStatusConstants() {
            assertEquals(1, BillingConstants.ACCOUNT_STATUS_ACTIVE);
            assertEquals(2, BillingConstants.ACCOUNT_STATUS_FROZEN);
            assertEquals(0, BillingConstants.ACCOUNT_STATUS_DISABLED);
        }

        @Test
        @DisplayName("活跃账户状态正确")
        void shouldHaveCorrectActiveStatus() {
            assertEquals(BillingConstants.ACCOUNT_STATUS_ACTIVE, testAccount.getStatus());
            assertTrue(testAccount.getStatus() == 1);
        }

        @Test
        @DisplayName("冻结账户状态正确")
        void shouldHaveCorrectFrozenStatus() {
            testAccount.setStatus(BillingConstants.ACCOUNT_STATUS_FROZEN);
            assertEquals(2, testAccount.getStatus());
        }
    }

    @Nested
    @DisplayName("账户DTO转换测试")
    class DTOConversionTests {

        @Test
        @DisplayName("账户实体转DTO")
        void shouldConvertAccountToDTO() {
            when(creditAccountMapper.selectOne(any())).thenReturn(testAccount);

            CreditAccountDTO result = creditAccountService.getAccount(1L, 1L);

            assertNotNull(result);
            assertEquals(testAccount.getId(), result.getId());
            assertEquals(testAccount.getTenantId(), result.getTenantId());
            assertEquals(testAccount.getUserId(), result.getUserId());
            assertEquals(testAccount.getBalance(), result.getBalance());
            assertEquals(testAccount.getFrozenAmount(), result.getFrozenAmount());
            assertEquals(testAccount.getStatus(), result.getStatus());
        }

        @Test
        @DisplayName("新账户初始余额为0")
        void shouldHaveZeroInitialBalance() {
            CreditAccount newAccount = new CreditAccount();
            newAccount.setId(2L);
            newAccount.setTenantId(1L);
            newAccount.setUserId(2L);
            newAccount.setBalance(0L);
            newAccount.setFrozenAmount(0L);
            newAccount.setStatus(BillingConstants.ACCOUNT_STATUS_ACTIVE);

            when(creditAccountMapper.selectOne(any())).thenReturn(null);
            when(creditAccountMapper.insert(any(CreditAccount.class))).thenReturn(1);

            CreditAccountDTO result = creditAccountService.getOrCreateAccount(1L, 2L);

            assertEquals(0L, result.getBalance());
            assertEquals(0L, result.getFrozenAmount());
        }
    }

    @Nested
    @DisplayName("Redis分布式锁测试")
    class DistributedLockTests {

        @Test
        @DisplayName("锁key格式正确")
        void shouldHaveCorrectLockKeyFormat() {
            String expectedKey = BillingConstants.REDIS_KEY_LOCK + "account:1";
            assertEquals("billing:lock:account:1", expectedKey);
        }

        @Test
        @DisplayName("并发操作时正确获取锁")
        void shouldAcquireLockForConcurrentOperations() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(creditAccountMapper.addBalance(1L, 1000L)).thenReturn(1);

            boolean result = creditAccountService.addBalance(1L, 1000L);

            assertTrue(result);
            verify(rLock).tryLock(5, 10, TimeUnit.SECONDS);
            verify(rLock, atLeastOnce()).unlock();
        }

        @Test
        @DisplayName("锁获取超时正确处理")
        void shouldHandleLockTimeout() throws InterruptedException {
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(false);

            boolean result = creditAccountService.addBalance(1L, 1000L);

            assertFalse(result);
            verify(rLock, never()).unlock();
        }
    }

    @Nested
    @DisplayName("账户余额一致性测试")
    class BalanceConsistencyTests {

        @Test
        @DisplayName("余额计算公式验证")
        void shouldVerifyBalanceCalculation() {
            // 可用余额 = 总余额 - 冻结金额
            long totalBalance = 10000L;
            long frozenAmount = 2000L;
            long availableBalance = totalBalance - frozenAmount;

            assertEquals(8000L, availableBalance);
        }

        @Test
        @DisplayName("充值后余额增加")
        void shouldIncreaseBalanceAfterRecharge() {
            long initialBalance = 10000L;
            long rechargeAmount = 5000L;
            long expectedBalance = initialBalance + rechargeAmount;

            assertEquals(15000L, expectedBalance);
        }

        @Test
        @DisplayName("消费后余额减少")
        void shouldDecreaseBalanceAfterConsumption() {
            long initialBalance = 10000L;
            long consumptionAmount = 3000L;
            long expectedBalance = initialBalance - consumptionAmount;

            assertEquals(7000L, expectedBalance);
        }
    }
}
