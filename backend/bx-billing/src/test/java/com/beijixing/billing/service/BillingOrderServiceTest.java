package com.beijixing.billing.service;

import com.beijixing.billing.constants.BillingConstants;
import com.beijixing.billing.dto.BillingOrderDTO;
import com.beijixing.billing.dto.RechargeRequestDTO;
import com.beijixing.billing.entity.BillingOrder;
import com.beijixing.billing.entity.CreditAccount;
import com.beijixing.billing.mapper.BillingOrderMapper;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * BillingOrderService 单元测试
 * 测试订单创建、支付处理、取消等核心功能
 *
 * @author 测试工程师 (EMP-QA-001)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("订单服务测试")
class BillingOrderServiceTest {

    @Mock
    private BillingOrderMapper orderMapper;

    @Mock
    private CreditAccountMapper creditAccountMapper;

    @Mock
    private CreditAccountService creditAccountService;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private BillingOrderService orderService;

    private BillingOrder testOrder;
    private RechargeRequestDTO testRechargeRequest;

    @BeforeEach
    void setUp() {
        testOrder = createTestOrder();
        testRechargeRequest = createTestRechargeRequest();
    }

    private BillingOrder createTestOrder() {
        BillingOrder order = new BillingOrder();
        order.setId(1L);
        order.setOrderNo("BX20240408123456ABC123");
        order.setTenantId(1L);
        order.setUserId(1L);
        order.setOrderType(BillingConstants.ORDER_TYPE_RECHARGE);
        order.setStatus(BillingConstants.ORDER_STATUS_PENDING);
        order.setPayType(BillingConstants.PAY_TYPE_WECHAT);
        order.setAmount(10000L);
        order.setActualAmount(10000L);
        order.setBonusAmount(500L);
        order.setExpireTime(LocalDateTime.now().plusMinutes(30));
        order.setCreateTime(LocalDateTime.now());
        return order;
    }

    private RechargeRequestDTO createTestRechargeRequest() {
        RechargeRequestDTO request = new RechargeRequestDTO();
        request.setUserId(1L);
        request.setAmount(10000L);
        request.setPayType(BillingConstants.PAY_TYPE_WECHAT);
        request.setDescription("测试充值");
        return request;
    }

    @Nested
    @DisplayName("充值订单创建测试")
    class CreateRechargeOrderTests {

        @Test
        @DisplayName("创建充值订单成功")
        void shouldCreateRechargeOrderSuccessfully() {
            when(orderMapper.insert(any(BillingOrder.class))).thenReturn(1);

            BillingOrderDTO result = orderService.createRechargeOrder(1L, testRechargeRequest);

            assertNotNull(result);
            assertNotNull(result.getOrderNo());
            assertTrue(result.getOrderNo().startsWith("BX"));
            assertEquals(BillingConstants.ORDER_TYPE_RECHARGE, result.getOrderType());
            assertEquals(BillingConstants.ORDER_STATUS_PENDING, result.getStatus());
            assertEquals(10000L, result.getAmount());
            verify(orderMapper, times(1)).insert(any(BillingOrder.class));
        }

        @Test
        @DisplayName("创建订单 - 包含赠送金额")
        void shouldIncludeBonusAmount() {
            when(orderMapper.insert(any(BillingOrder.class))).thenAnswer(invocation -> {
                BillingOrder order = invocation.getArgument(0);
                assertEquals(500L, order.getBonusAmount());
                assertEquals(10000L + 500L, order.getActualAmount());
                return 1;
            });

            BillingOrderDTO result = orderService.createRechargeOrder(1L, testRechargeRequest);

            assertNotNull(result);
            assertEquals(500L, result.getBonusAmount());
        }

        @Test
        @DisplayName("创建订单 - 设置过期时间")
        void shouldSetOrderExpiryTime() {
            when(orderMapper.insert(any(BillingOrder.class))).thenAnswer(invocation -> {
                BillingOrder order = invocation.getArgument(0);
                assertNotNull(order.getExpireTime());
                assertTrue(order.getExpireTime().isAfter(LocalDateTime.now()));
                return 1;
            });

            orderService.createRechargeOrder(1L, testRechargeRequest);

            verify(orderMapper).insert(any(BillingOrder.class));
        }
    }

    @Nested
    @DisplayName("支付回调处理测试")
    class PaymentCallbackTests {

        @Test
        @DisplayName("支付回调成功处理")
        void shouldHandlePaymentCallbackSuccessfully() throws InterruptedException {
            when(orderMapper.selectByOrderNo("BX20240408123456ABC123")).thenReturn(testOrder);
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(true);
            when(rLock.isHeldByCurrentThread()).thenReturn(true);
            when(orderMapper.updateById(any(BillingOrder.class))).thenReturn(1);
            when(creditAccountMapper.selectOne(any())).thenReturn(createTestCreditAccount());
            when(creditAccountService.addBalance(1L, 10500L)).thenReturn(true);

            boolean result = orderService.handlePaymentCallback(
                    "BX20240408123456ABC123", "WX123456789", BillingConstants.PAY_TYPE_WECHAT);

            assertTrue(result);
            verify(orderMapper).updateById(any(BillingOrder.class));
            verify(creditAccountService).addBalance(1L, 10500L);
        }

        @Test
        @DisplayName("支付回调 - 订单不存在")
        void shouldFailWhenOrderNotFound() {
            when(orderMapper.selectByOrderNo("NONEXISTENT")).thenReturn(null);

            boolean result = orderService.handlePaymentCallback(
                    "NONEXISTENT", "WX123456789", BillingConstants.PAY_TYPE_WECHAT);

            assertFalse(result);
        }

        @Test
        @DisplayName("支付回调 - 订单已支付")
        void shouldFailWhenOrderAlreadyPaid() {
            testOrder.setStatus(BillingConstants.ORDER_STATUS_PAID);
            when(orderMapper.selectByOrderNo("BX20240408123456ABC123")).thenReturn(testOrder);

            boolean result = orderService.handlePaymentCallback(
                    "BX20240408123456ABC123", "WX123456789", BillingConstants.PAY_TYPE_WECHAT);

            assertFalse(result);
        }

        @Test
        @DisplayName("支付回调 - 获取锁失败")
        void shouldFailWhenLockAcquisitionFails() throws InterruptedException {
            when(orderMapper.selectByOrderNo("BX20240408123456ABC123")).thenReturn(testOrder);
            when(redissonClient.getLock(anyString())).thenReturn(rLock);
            when(rLock.tryLock(5, 30, TimeUnit.SECONDS)).thenReturn(false);

            boolean result = orderService.handlePaymentCallback(
                    "BX20240408123456ABC123", "WX123456789", BillingConstants.PAY_TYPE_WECHAT);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("订单取消测试")
    class OrderCancellationTests {

        @Test
        @DisplayName("取消待支付订单成功")
        void shouldCancelPendingOrderSuccessfully() {
            when(orderMapper.selectByOrderNo("BX20240408123456ABC123")).thenReturn(testOrder);
            when(orderMapper.updateById(any(BillingOrder.class))).thenReturn(1);

            boolean result = orderService.cancelOrder("BX20240408123456ABC123");

            assertTrue(result);
            verify(orderMapper).updateById(argThat(order ->
                    order.getStatus() == BillingConstants.ORDER_STATUS_CANCELLED
            ));
        }

        @Test
        @DisplayName("取消订单 - 订单不存在")
        void shouldFailWhenCancelNonExistentOrder() {
            when(orderMapper.selectByOrderNo("NONEXISTENT")).thenReturn(null);

            boolean result = orderService.cancelOrder("NONEXISTENT");

            assertFalse(result);
        }

        @Test
        @DisplayName("取消订单 - 已支付订单不能取消")
        void shouldFailWhenCancelPaidOrder() {
            testOrder.setStatus(BillingConstants.ORDER_STATUS_PAID);
            when(orderMapper.selectByOrderNo("BX20240408123456ABC123")).thenReturn(testOrder);

            boolean result = orderService.cancelOrder("BX20240408123456ABC123");

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("订单查询测试")
    class OrderQueryTests {

        @Test
        @DisplayName("根据订单号查询订单")
        void shouldGetOrderByOrderNo() {
            when(orderMapper.selectByOrderNo("BX20240408123456ABC123")).thenReturn(testOrder);

            BillingOrderDTO result = orderService.getOrderByNo("BX20240408123456ABC123");

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("BX20240408123456ABC123", result.getOrderNo());
        }

        @Test
        @DisplayName("查询不存在的订单返回null")
        void shouldReturnNullWhenOrderNotFound() {
            when(orderMapper.selectByOrderNo("NONEXISTENT")).thenReturn(null);

            BillingOrderDTO result = orderService.getOrderByNo("NONEXISTENT");

            assertNull(result);
        }

        @Test
        @DisplayName("查询用户充值订单列表")
        void shouldGetUserRechargeOrders() {
            List<BillingOrder> orders = Arrays.asList(testOrder);
            when(orderMapper.selectList(any())).thenReturn(orders);

            List<BillingOrderDTO> result = orderService.getUserOrders(1L, BillingConstants.ORDER_TYPE_RECHARGE);

            assertNotNull(result);
            assertEquals(1, result.size());
        }
    }

    @Nested
    @DisplayName("充值赠送计算测试")
    class BonusCalculationTests {

        @Test
        @DisplayName("充值10000 - 最低档赠送500")
        void shouldCalculateBonusFor10000() {
            Long bonus = orderService.calculateBonus(10000L);
            assertEquals(500L, bonus);
        }

        @Test
        @DisplayName("充值50000 - 第二档赠送5000")
        void shouldCalculateBonusFor50000() {
            Long bonus = orderService.calculateBonus(50000L);
            assertEquals(5000L, bonus);
        }

        @Test
        @DisplayName("充值100000 - 第三档赠送15000")
        void shouldCalculateBonusFor100000() {
            Long bonus = orderService.calculateBonus(100000L);
            assertEquals(15000L, bonus);
        }

        @Test
        @DisplayName("充值500000 - 最高档赠送100000")
        void shouldCalculateBonusFor500000() {
            Long bonus = orderService.calculateBonus(500000L);
            assertEquals(100000L, bonus);
        }

        @Test
        @DisplayName("充值低于最低档无赠送")
        void shouldReturnZeroBonusForSmallAmount() {
            Long bonus = orderService.calculateBonus(1000L);
            assertEquals(0L, bonus);
        }

        @Test
        @DisplayName("充值超过最高档使用最高档赠送")
        void shouldCapBonusAtHighestTier() {
            Long bonus = orderService.calculateBonus(1000000L);
            assertEquals(100000L, bonus); // 封顶100000
        }
    }

    @Nested
    @DisplayName("订单状态常量测试")
    class OrderStatusTests {

        @Test
        @DisplayName("订单状态枚举正确")
        void shouldHaveCorrectOrderStatusConstants() {
            assertEquals(0, BillingConstants.ORDER_STATUS_PENDING);
            assertEquals(1, BillingConstants.ORDER_STATUS_PAID);
            assertEquals(2, BillingConstants.ORDER_STATUS_CANCELLED);
            assertEquals(3, BillingConstants.ORDER_STATUS_REFUNDED);
        }

        @Test
        @DisplayName("支付类型常量正确")
        void shouldHaveCorrectPayTypeConstants() {
            assertEquals(1, BillingConstants.PAY_TYPE_WECHAT);
            assertEquals(2, BillingConstants.PAY_TYPE_ALIPAY);
            assertEquals(3, BillingConstants.PAY_TYPE_BALANCE);
        }

        @Test
        @DisplayName("订单类型常量正确")
        void shouldHaveCorrectOrderTypeConstants() {
            assertEquals(1, BillingConstants.ORDER_TYPE_RECHARGE);
            assertEquals(2, BillingConstants.ORDER_TYPE_CONSUMPTION);
            assertEquals(3, BillingConstants.ORDER_TYPE_PACKAGE);
            assertEquals(4, BillingConstants.ORDER_TYPE_REFUND);
        }
    }

    @Nested
    @DisplayName("订单号生成测试")
    class OrderNoGenerationTests {

        @Test
        @DisplayName("订单号格式正确")
        void shouldGenerateCorrectOrderNoFormat() {
            when(orderMapper.insert(any(BillingOrder.class))).thenReturn(1);

            BillingOrderDTO result = orderService.createRechargeOrder(1L, testRechargeRequest);

            assertNotNull(result.getOrderNo());
            assertTrue(result.getOrderNo().startsWith("BX"));
            assertEquals(26, result.getOrderNo().length()); // BX + 14位时间 + 6位UUID
        }
    }

    private CreditAccount createTestCreditAccount() {
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
}
