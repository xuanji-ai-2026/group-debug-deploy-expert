package com.beijixing.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.beijixing.content.entity.Content;
import com.beijixing.content.entity.ContentPublishRecord;
import com.beijixing.content.enums.*;
import com.beijixing.content.mapper.ContentMapper;
import com.beijixing.content.mapper.ContentPublishRecordMapper;
import com.beijixing.content.service.impl.ContentPublishServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContentPublishService 单元测试
 * 测试多平台发布、发布重试、撤回等核心功能
 *
 * @author 测试工程师 (EMP-QA-001)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("内容发布服务测试")
class ContentPublishServiceTest {

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private ContentPublishRecordMapper publishRecordMapper;

    @InjectMocks
    private ContentPublishServiceImpl publishService;

    private Content testContent;

    @BeforeEach
    void setUp() {
        testContent = createTestContent();
    }

    private Content createTestContent() {
        Content content = new Content();
        content.setId(1L);
        content.setTitle("测试文章标题");
        content.setContent("这是测试文章内容");
        content.setContentType(ContentType.ARTICLE.getCode());
        content.setStatus(ContentStatus.PUBLISHED.getCode());
        content.setPublishStatus(PublishStatus.UNPUBLISHED.getCode());
        content.setAuthorId(1L);
        content.setAuthorName("测试作者");
        return content;
    }

    private ContentPublishRecord createPublishRecord(Long id, int platform, int status) {
        ContentPublishRecord record = new ContentPublishRecord();
        record.setId(id);
        record.setContentId(1L);
        record.setPlatform(platform);
        record.setStatus(status);
        record.setRetryCount(0);
        record.setMaxRetryCount(3);
        record.setPublishTime(LocalDateTime.now());
        return record;
    }

    @Nested
    @DisplayName("单平台发布测试")
    class SinglePlatformPublishTests {

        @Test
        @DisplayName("发布到微信公众号成功")
        void shouldPublishToWechatSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenAnswer(invocation -> {
                ContentPublishRecord r = invocation.getArgument(0);
                r.setId(1L);
                return 1;
            });

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.singletonList(PublishPlatform.WECHAT.getCode()));
            });

            verify(publishRecordMapper, times(1)).insert(any(ContentPublishRecord.class));
        }

        @Test
        @DisplayName("发布到微博成功")
        void shouldPublishToWeiboSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.singletonList(PublishPlatform.WEIBO.getCode()));
            });
        }

        @Test
        @DisplayName("发布到抖音成功")
        void shouldPublishToDouyinSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.singletonList(PublishPlatform.DOUYIN.getCode()));
            });
        }

        @Test
        @DisplayName("发布到小红书成功")
        void shouldPublishToXiaohongshuSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.singletonList(PublishPlatform.XIAOHONGSHU.getCode()));
            });
        }

        @Test
        @DisplayName("发布到B站成功")
        void shouldPublishToBilibiliSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.singletonList(PublishPlatform.BILIBILI.getCode()));
            });
        }

        @Test
        @DisplayName("发布到官网成功")
        void shouldPublishToWebsiteSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.singletonList(PublishPlatform.WEBSITE.getCode()));
            });
        }

        @Test
        @DisplayName("发布 - 内容不存在抛出异常")
        void shouldThrowExceptionWhenContentNotFound() {
            when(contentMapper.selectById(999L)).thenReturn(null);

            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                publishService.publishToPlatforms(999L, Collections.singletonList(1));
            });

            assertEquals("内容不存在", exception.getMessage());
        }

        @Test
        @DisplayName("发布 - 空平台列表不处理")
        void shouldDoNothingWithEmptyPlatformList() {
            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, Collections.emptyList());
            });

            verify(publishRecordMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("多平台同时发布测试")
    class MultiPlatformPublishTests {

        @Test
        @DisplayName("同时发布到多个平台")
        void shouldPublishToMultiplePlatforms() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);

            List<Integer> platforms = Arrays.asList(
                    PublishPlatform.WECHAT.getCode(),
                    PublishPlatform.WEIBO.getCode(),
                    PublishPlatform.DOUYIN.getCode()
            );

            assertDoesNotThrow(() -> {
                publishService.publishToPlatforms(1L, platforms);
            });

            verify(publishRecordMapper, times(3)).insert(any(ContentPublishRecord.class));
        }
    }

    @Nested
    @DisplayName("发布记录管理测试")
    class PublishRecordManagementTests {

        @Test
        @DisplayName("获取发布记录成功")
        void shouldGetPublishStatus() {
            ContentPublishRecord record = createPublishRecord(1L, 1, PublishRecordStatus.SUCCESS.getCode());
            when(publishRecordMapper.selectById(1L)).thenReturn(record);

            ContentPublishRecord result = publishService.getPublishStatus(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(PublishRecordStatus.SUCCESS.getCode(), result.getStatus());
        }

        @Test
        @DisplayName("发布状态枚举正确")
        void shouldHaveCorrectPublishStatusEnums() {
            assertEquals("待发布", PublishRecordStatus.PENDING.getName());
            assertEquals("发布中", PublishRecordStatus.PUBLISHING.getName());
            assertEquals("成功", PublishRecordStatus.SUCCESS.getName());
            assertEquals("失败", PublishRecordStatus.FAILED.getName());
        }
    }

    @Nested
    @DisplayName("发布重试测试")
    class PublishRetryTests {

        @Test
        @DisplayName("失败记录自动重试")
        void shouldRetryFailedRecords() {
            ContentPublishRecord failedRecord = createPublishRecord(1L, 1, PublishRecordStatus.FAILED.getCode());
            failedRecord.setRetryCount(1);
            failedRecord.setMaxRetryCount(3);

            when(publishRecordMapper.selectRetryRecords()).thenReturn(Collections.singletonList(failedRecord));
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(publishRecordMapper.selectById(1L)).thenReturn(failedRecord);
            when(publishRecordMapper.updateById(any(ContentPublishRecord.class))).thenReturn(1);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            assertDoesNotThrow(() -> {
                publishService.retryFailedPublishes();
            });
        }

        @Test
        @DisplayName("达到最大重试次数后不再重试")
        void shouldNotRetryAfterMaxAttempts() {
            ContentPublishRecord maxRetryRecord = createPublishRecord(1L, 1, PublishRecordStatus.FAILED.getCode());
            maxRetryRecord.setRetryCount(3);
            maxRetryRecord.setMaxRetryCount(3);

            when(publishRecordMapper.selectRetryRecords()).thenReturn(Collections.singletonList(maxRetryRecord));
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(publishRecordMapper.selectById(1L)).thenReturn(maxRetryRecord);
            when(publishRecordMapper.updateById(any(ContentPublishRecord.class))).thenReturn(1);

            publishService.retryFailedPublishes();

            verify(publishRecordMapper).updateById(argThat(record ->
                record.getRetryCount() == 3 && record.getStatus() == PublishRecordStatus.FAILED.getCode()
            ));
        }

        @Test
        @DisplayName("无失败记录时不执行重试")
        void shouldDoNothingWhenNoFailedRecords() {
            when(publishRecordMapper.selectRetryRecords()).thenReturn(Collections.emptyList());

            publishService.retryFailedPublishes();

            verify(contentMapper, never()).selectById(anyLong());
        }

        @Test
        @DisplayName("重试时内容不存在跳过")
        void shouldSkipWhenContentNotFoundOnRetry() {
            ContentPublishRecord record = createPublishRecord(1L, 1, PublishRecordStatus.FAILED.getCode());
            when(publishRecordMapper.selectRetryRecords()).thenReturn(Collections.singletonList(record));
            when(contentMapper.selectById(1L)).thenReturn(null);

            publishService.retryFailedPublishes();

            verify(publishRecordMapper, never()).updateById(any());
        }
    }

    @Nested
    @DisplayName("内容撤回测试")
    class WithdrawTests {

        @Test
        @DisplayName("从平台撤回内容成功")
        void shouldWithdrawFromPlatformSuccessfully() {
            ContentPublishRecord record1 = createPublishRecord(1L, 1, PublishRecordStatus.SUCCESS.getCode());
            ContentPublishRecord record2 = createPublishRecord(2L, 2, PublishRecordStatus.SUCCESS.getCode());

            when(publishRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(record1, record2));
            when(publishRecordMapper.updateById(any(ContentPublishRecord.class))).thenReturn(1);

            boolean result = publishService.withdrawFromPlatform(1L, 1);

            assertTrue(result);
            verify(publishRecordMapper, times(1)).updateById(any(ContentPublishRecord.class));
        }

        @Test
        @DisplayName("撤回 - 无对应发布记录")
        void shouldHandleNoPublishRecordOnWithdraw() {
            when(publishRecordMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            boolean result = publishService.withdrawFromPlatform(1L, 99);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("批量发布测试")
    class BatchPublishTests {

        @Test
        @DisplayName("批量发布多个内容")
        void shouldBatchPublishMultipleContents() {
            Content content1 = createTestContent();
            content1.setId(1L);
            Content content2 = createTestContent();
            content2.setId(2L);

            when(contentMapper.selectById(1L)).thenReturn(content1);
            when(contentMapper.selectById(2L)).thenReturn(content2);
            when(publishRecordMapper.insert(any(ContentPublishRecord.class))).thenReturn(1);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            List<Integer> platforms = Arrays.asList(1, 2);

            assertDoesNotThrow(() -> {
                publishService.batchPublish(Arrays.asList(1L, 2L), platforms);
            });

            verify(publishRecordMapper, times(4)).insert(any(ContentPublishRecord.class));
        }

        @Test
        @DisplayName("批量发布 - 空ID列表不处理")
        void shouldDoNothingWithEmptyIdList() {
            assertDoesNotThrow(() -> {
                publishService.batchPublish(Collections.emptyList(), Arrays.asList(1, 2));
            });

            verify(contentMapper, never()).selectById(anyLong());
        }
    }

    @Nested
    @DisplayName("发布平台枚举测试")
    class PlatformEnumTests {

        @Test
        @DisplayName("所有平台枚举定义正确")
        void shouldHaveCorrectPlatformEnums() {
            assertNotNull(PublishPlatform.WECHAT);
            assertNotNull(PublishPlatform.WEIBO);
            assertNotNull(PublishPlatform.DOUYIN);
            assertNotNull(PublishPlatform.XIAOHONGSHU);
            assertNotNull(PublishPlatform.BILIBILI);
            assertNotNull(PublishPlatform.WEBSITE);
        }

        @Test
        @DisplayName("平台名称正确")
        void shouldHaveCorrectPlatformNames() {
            assertEquals("微信公众号", PublishPlatform.WECHAT.getName());
            assertEquals("微博", PublishPlatform.WEIBO.getName());
            assertEquals("抖音", PublishPlatform.DOUYIN.getName());
            assertEquals("小红书", PublishPlatform.XIAOHONGSHU.getName());
            assertEquals("B站", PublishPlatform.BILIBILI.getName());
            assertEquals("官网", PublishPlatform.WEBSITE.getName());
        }
    }

    @Nested
    @DisplayName("发布状态流转测试")
    class PublishStatusFlowTests {

        @Test
        @DisplayName("发布状态从待发布到成功")
        void shouldTransitionFromPendingToSuccess() {
            ContentPublishRecord record = createPublishRecord(1L, 1, PublishRecordStatus.PENDING.getCode());
            assertEquals(PublishRecordStatus.PENDING.getCode(), record.getStatus());

            record.setStatus(PublishRecordStatus.SUCCESS.getCode());
            assertEquals(PublishRecordStatus.SUCCESS.getCode(), record.getStatus());
        }

        @Test
        @DisplayName("发布状态从发布中到失败")
        void shouldTransitionFromPublishingToFailed() {
            ContentPublishRecord record = createPublishRecord(1L, 1, PublishRecordStatus.PUBLISHING.getCode());
            assertEquals(PublishRecordStatus.PUBLISHING.getCode(), record.getStatus());

            record.setStatus(PublishRecordStatus.FAILED.getCode());
            record.setErrorMsg("网络错误");
            assertEquals(PublishRecordStatus.FAILED.getCode(), record.getStatus());
            assertEquals("网络错误", record.getErrorMsg());
        }
    }
}
