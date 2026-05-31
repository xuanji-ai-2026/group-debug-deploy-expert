package com.beijixing.content.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.content.dto.ContentAuditDTO;
import com.beijixing.content.dto.ContentDTO;
import com.beijixing.content.dto.ContentQueryDTO;
import com.beijixing.content.entity.*;
import com.beijixing.content.enums.*;
import com.beijixing.content.exception.ContentException;
import com.beijixing.content.mapper.*;
import com.beijixing.content.service.impl.ContentServiceImpl;
import com.beijixing.content.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ContentService 单元测试
 * 测试内容创建、更新、发布、审核等核心功能
 *
 * @author 测试工程师 (EMP-QA-001)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("内容服务测试")
class ContentServiceTest {

    @Mock
    private ContentMapper contentMapper;

    @Mock
    private ContentVersionMapper versionMapper;

    @Mock
    private ContentTagMapper tagMapper;

    @Mock
    private ContentTagRelationMapper tagRelationMapper;

    @Mock
    private ContentAuditRecordMapper auditRecordMapper;

    @Mock
    private ContentPublishRecordMapper publishRecordMapper;

    @Mock
    private ContentPublishService publishService;

    @Mock
    private RedissonClient redissonClient;

    @InjectMocks
    private ContentServiceImpl contentService;

    private Content testContent;
    private ContentDTO testContentDTO;

    @BeforeEach
    void setUp() {
        testContent = createTestContent();
        testContentDTO = createTestContentDTO();
    }

    private Content createTestContent() {
        Content content = new Content();
        content.setId(1L);
        content.setTitle("测试文章标题");
        content.setContent("这是测试文章内容");
        content.setSummary("文章摘要");
        content.setCoverImage("https://example.com/cover.jpg");
        content.setContentType(ContentType.ARTICLE.getCode());
        content.setStatus(ContentStatus.DRAFT.getCode());
        content.setPublishStatus(PublishStatus.UNPUBLISHED.getCode());
        // 注意：Content类的浏览/点赞等字段类型为Integer，这里不能使用long类型，否则会编译错误
        // 业务上单内容浏览量不会超过Integer.MAX_VALUE(21亿)，使用Integer完全满足需求
        content.setViewCount(0);
        content.setLikeCount(0);
        content.setCommentCount(0);
        content.setShareCount(0);
        content.setVersion(1);
        content.setAuthorId(1L);
        content.setAuthorName("测试作者");
        content.setTags("[\"标签1\",\"标签2\"]");
        content.setDeleted(0);
        return content;
    }

    private ContentDTO createTestContentDTO() {
        ContentDTO dto = new ContentDTO();
        dto.setTitle("测试文章标题");
        dto.setContent("这是测试文章内容");
        dto.setSummary("文章摘要");
        dto.setCoverImage("https://example.com/cover.jpg");
        dto.setContentType(ContentType.ARTICLE.getCode());
        dto.setTags(Arrays.asList("标签1", "标签2"));
        return dto;
    }

    @Nested
    @DisplayName("内容创建测试")
    class CreateContentTests {

        @Test
        @DisplayName("创建内容成功")
        void shouldCreateContentSuccessfully() {
            when(contentMapper.insert(any(Content.class))).thenReturn(1);
            when(tagMapper.selectByName(anyString())).thenReturn(null);
            when(tagMapper.insert(any(ContentTag.class))).thenReturn(1);
            when(tagMapper.incrementUsageCount(anyLong())).thenReturn(1);
            when(tagRelationMapper.insert(any(ContentTagRelation.class))).thenReturn(1);
            when(versionMapper.insert(any(ContentVersion.class))).thenReturn(1);

            ContentVO result = contentService.createContent(testContentDTO);

            assertNotNull(result);
            assertEquals(testContentDTO.getTitle(), result.getTitle());
            verify(contentMapper, times(1)).insert(any(Content.class));
            verify(tagRelationMapper, times(2)).insert(any(ContentTagRelation.class));
            verify(versionMapper, times(1)).insert(any(ContentVersion.class));
        }

        @Test
        @DisplayName("创建内容 - 高风险敏感词拒绝")
        void shouldRejectHighRiskSensitiveContent() {
            ContentDTO riskyContent = new ContentDTO();
            riskyContent.setTitle("正常标题");
            riskyContent.setContent("违禁词1 违禁词2 违禁词3 违禁词4 违禁词5 违禁词6 违禁词7");

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.createContent(riskyContent);
            });

            assertEquals("内容包含高风险违禁词，请修改后再提交", exception.getMessage());
        }

        @Test
        @DisplayName("创建内容 - 默认状态为草稿")
        void shouldSetDefaultDraftStatus() {
            when(contentMapper.insert(any(Content.class))).thenAnswer(invocation -> {
                Content c = invocation.getArgument(0);
                assertEquals(ContentStatus.DRAFT.getCode(), c.getStatus());
                assertEquals(PublishStatus.UNPUBLISHED.getCode(), c.getPublishStatus());
                assertEquals(1, c.getVersion());
                return 1;
            });
            when(tagRelationMapper.insert(any(ContentTagRelation.class))).thenReturn(1);
            when(versionMapper.insert(any(ContentVersion.class))).thenReturn(1);

            contentService.createContent(testContentDTO);

            verify(contentMapper).insert(any(Content.class));
        }

        @Test
        @DisplayName("创建内容 - 标签处理")
        void shouldProcessTagsOnCreate() {
            when(contentMapper.insert(any(Content.class))).thenReturn(1);
            when(tagMapper.selectByName(anyString())).thenReturn(null);
            when(tagMapper.insert(any(ContentTag.class))).thenReturn(1);
            when(tagMapper.incrementUsageCount(anyLong())).thenReturn(1);
            when(tagRelationMapper.insert(any(ContentTagRelation.class))).thenReturn(1);
            when(versionMapper.insert(any(ContentVersion.class))).thenReturn(1);

            contentService.createContent(testContentDTO);

            // 验证创建了2个标签关联（对应2个标签）
            verify(tagRelationMapper, times(2)).insert(any(ContentTagRelation.class));
        }
    }

    @Nested
    @DisplayName("内容更新测试")
    class UpdateContentTests {

        @Test
        @DisplayName("更新内容成功")
        void shouldUpdateContentSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(tagMapper.selectByName(anyString())).thenReturn(null);
            when(tagMapper.insert(any(ContentTag.class))).thenReturn(1);
            when(tagMapper.incrementUsageCount(anyLong())).thenReturn(1);
            when(tagRelationMapper.deleteByContentId(1L)).thenReturn(2);
            when(tagRelationMapper.insert(any(ContentTagRelation.class))).thenReturn(1);
            when(versionMapper.insert(any(ContentVersion.class))).thenReturn(1);

            ContentDTO updateDTO = createTestContentDTO();
            updateDTO.setId(1L);
            updateDTO.setTitle("更新后的标题");

            ContentVO result = contentService.updateContent(updateDTO);

            assertNotNull(result);
            verify(contentMapper).updateById(any(Content.class));
            verify(tagRelationMapper).deleteByContentId(1L);
        }

        @Test
        @DisplayName("更新内容 - ID为空抛出异常")
        void shouldThrowExceptionWhenIdIsNull() {
            ContentDTO noIdDTO = createTestContentDTO();
            noIdDTO.setId(null);

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.updateContent(noIdDTO);
            });

            assertEquals("内容ID不能为空", exception.getMessage());
        }

        @Test
        @DisplayName("更新内容 - 内容不存在抛出异常")
        void shouldThrowExceptionWhenContentNotFound() {
            when(contentMapper.selectById(999L)).thenReturn(null);

            ContentDTO updateDTO = createTestContentDTO();
            updateDTO.setId(999L);

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.updateContent(updateDTO);
            });

            assertEquals("内容不存在", exception.getMessage());
        }

        @Test
        @DisplayName("更新内容 - 版本号递增")
        void shouldIncrementVersionOnUpdate() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenAnswer(invocation -> {
                Content c = invocation.getArgument(0);
                assertEquals(testContent.getVersion() + 1, c.getVersion());
                return 1;
            });
            when(tagRelationMapper.deleteByContentId(1L)).thenReturn(0);
            when(versionMapper.insert(any(ContentVersion.class))).thenReturn(1);

            ContentDTO updateDTO = createTestContentDTO();
            updateDTO.setId(1L);

            contentService.updateContent(updateDTO);

            verify(contentMapper).updateById(any(Content.class));
        }
    }

    @Nested
    @DisplayName("内容删除测试")
    class DeleteContentTests {

        @Test
        @DisplayName("删除内容成功")
        void shouldDeleteContentSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.deleteById(1L)).thenReturn(1);
            when(tagRelationMapper.deleteByContentId(1L)).thenReturn(2);

            contentService.deleteContent(1L);

            verify(contentMapper).deleteById(1L);
            verify(tagRelationMapper).deleteByContentId(1L);
        }

        @Test
        @DisplayName("删除内容 - 内容不存在抛出异常")
        void shouldThrowExceptionWhenDeletingNonExistent() {
            when(contentMapper.selectById(999L)).thenReturn(null);

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.deleteContent(999L);
            });

            assertEquals("内容不存在", exception.getMessage());
        }

        @Test
        @DisplayName("批量删除成功")
        void shouldBatchDeleteSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.selectById(2L)).thenReturn(testContent);
            when(contentMapper.deleteById(anyLong())).thenReturn(1);
            when(tagRelationMapper.deleteByContentId(anyLong())).thenReturn(2);

            contentService.batchDelete(Arrays.asList(1L, 2L));

            verify(contentMapper, times(2)).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("内容查询测试")
    class QueryContentTests {

        @Test
        @DisplayName("根据ID获取内容成功")
        void shouldGetContentById() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);

            ContentVO result = contentService.getContentById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("测试文章标题", result.getTitle());
        }

        @Test
        @DisplayName("根据ID获取 - 内容不存在抛出异常")
        void shouldThrowExceptionWhenGettingNonExistent() {
            when(contentMapper.selectById(999L)).thenReturn(null);

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.getContentById(999L);
            });

            assertEquals("内容不存在", exception.getMessage());
        }

        @Test
        @DisplayName("分页查询内容列表")
        void shouldListContentsByPage() {
            ContentQueryDTO query = new ContentQueryDTO();
            query.setPageNum(1);
            query.setPageSize(10);

            Page<ContentListVO> mockPage = new Page<>(1, 10);
            mockPage.setRecords(Arrays.asList(new ContentListVO(), new ContentListVO()));
            mockPage.setTotal(2);

            when(contentMapper.selectContentPage(any(Page.class), any(ContentQueryDTO.class)))
                    .thenReturn(mockPage);

            IPage<ContentListVO> result = contentService.listContents(query);

            assertNotNull(result);
            assertEquals(2, result.getTotal());
        }
    }

    @Nested
    @DisplayName("内容发布测试")
    class PublishContentTests {

        @Test
        @DisplayName("发布内容成功")
        void shouldPublishContentSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            List<Integer> platforms = Arrays.asList(1, 2); // 微信公众号、微博

            ContentVO result = contentService.publishContent(1L, platforms);

            assertNotNull(result);
            verify(contentMapper).updateById(any(Content.class));
            verify(publishService).publishToPlatforms(1L, platforms);
        }

        @Test
        @DisplayName("发布内容 - 内容不存在抛出异常")
        void shouldThrowExceptionWhenPublishingNonExistent() {
            when(contentMapper.selectById(999L)).thenReturn(null);

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.publishContent(999L, Arrays.asList(1));
            });

            assertEquals("内容不存在", exception.getMessage());
        }

        @Test
        @DisplayName("撤回内容成功")
        void shouldWithdrawContentSuccessfully() {
            testContent.setStatus(ContentStatus.PUBLISHED.getCode());
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            ContentVO result = contentService.withdrawContent(1L);

            assertNotNull(result);
            verify(contentMapper).updateById(any(Content.class));
        }

        @Test
        @DisplayName("置顶内容成功")
        void shouldToggleTopSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            ContentVO result = contentService.toggleTop(1L, true);

            assertNotNull(result);
            verify(contentMapper).updateById(any(Content.class));
        }

        @Test
        @DisplayName("取消置顶成功")
        void shouldUnpinSuccessfully() {
            testContent.setIsTop(true);
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            ContentVO result = contentService.toggleTop(1L, false);

            assertNotNull(result);
            verify(contentMapper).updateById(any(Content.class));
        }
    }

    @Nested
    @DisplayName("内容审核测试")
    class AuditContentTests {

        @Test
        @DisplayName("提交审核成功")
        void shouldSubmitForAuditSuccessfully() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(auditRecordMapper.insert(any(ContentAuditRecord.class))).thenReturn(1);

            ContentVO result = contentService.submitAudit(1L);

            assertNotNull(result);
            verify(auditRecordMapper).insert(any(ContentAuditRecord.class));
        }

        @Test
        @DisplayName("人工审核通过")
        void shouldApproveContent() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(auditRecordMapper.insert(any(ContentAuditRecord.class))).thenReturn(1);

            ContentAuditDTO dto = new ContentAuditDTO();
            dto.setContentId(1L);
            dto.setAuditResult(AuditResult.PASSED.getCode());
            dto.setAuditOpinion("内容合规，审核通过");

            ContentVO result = contentService.auditContent(dto);

            assertNotNull(result);
            verify(auditRecordMapper).insert(any(ContentAuditRecord.class));
        }

        @Test
        @DisplayName("人工审核拒绝")
        void shouldRejectContent() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(auditRecordMapper.insert(any(ContentAuditRecord.class))).thenReturn(1);

            ContentAuditDTO dto = new ContentAuditDTO();
            dto.setContentId(1L);
            dto.setAuditResult(AuditResult.REJECTED.getCode());
            dto.setAuditOpinion("内容含有违禁词");

            ContentVO result = contentService.auditContent(dto);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("版本管理测试")
    class VersionManagementTests {

        @Test
        @DisplayName("获取内容版本历史")
        void shouldGetContentVersions() {
            List<ContentVersion> versions = Arrays.asList(
                    new ContentVersion(), new ContentVersion()
            );
            when(versionMapper.selectByContentId(1L)).thenReturn(versions);

            List<ContentVersionVO> result = contentService.getContentVersions(1L);

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("回滚内容版本")
        void shouldRollbackVersion() {
            ContentVersion version = new ContentVersion();
            version.setContentId(1L);
            version.setVersion(1);
            version.setTitle("旧标题");
            version.setContent("旧内容");

            when(versionMapper.selectOne(any())).thenReturn(version);
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);
            when(versionMapper.insert(any(ContentVersion.class))).thenReturn(1);

            ContentVO result = contentService.rollbackVersion(1L, 1);

            assertNotNull(result);
            verify(contentMapper).updateById(any(Content.class));
        }

        @Test
        @DisplayName("回滚 - 版本不存在抛出异常")
        void shouldThrowExceptionWhenVersionNotFound() {
            when(versionMapper.selectOne(any())).thenReturn(null);

            ContentException exception = assertThrows(ContentException.class, () -> {
                contentService.rollbackVersion(1L, 99);
            });

            assertEquals("版本不存在", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("敏感词检测测试")
    class SensitiveWordCheckTests {

        @Test
        @DisplayName("无敏感词内容通过检测")
        void shouldPassWhenNoSensitiveWords() {
            String cleanContent = "这是一段正常的文章内容，不包含任何敏感词。";

            SensitiveWordCheckVO result = contentService.checkSensitiveWords(cleanContent);

            assertFalse(result.getHasSensitive());
            assertEquals(0, result.getRiskLevel());
        }

        @Test
        @DisplayName("含低风险敏感词检测")
        void shouldDetectLowRiskSensitiveWords() {
            String riskyContent = "这是一些包含违禁词1的内容。";

            SensitiveWordCheckVO result = contentService.checkSensitiveWords(riskyContent);

            assertTrue(result.getHasSensitive());
            assertEquals(1, result.getRiskLevel());
            assertTrue(result.getRiskLevelName().contains("低风险"));
        }

        @Test
        @DisplayName("含高风险敏感词检测")
        void shouldDetectHighRiskSensitiveWords() {
            String highRiskContent = "违禁词1 违禁词2 违禁词3 违禁词4 违禁词5 违禁词6 违禁词7";

            SensitiveWordCheckVO result = contentService.checkSensitiveWords(highRiskContent);

            assertTrue(result.getHasSensitive());
            assertEquals(3, result.getRiskLevel());
            assertEquals("高风险", result.getRiskLevelName());
        }
    }

    @Nested
    @DisplayName("定时发布测试")
    class SchedulePublishTests {

        @Test
        @DisplayName("创建定时发布任务")
        void shouldCreateSchedulePublishTask() {
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            LocalDateTime scheduledTime = LocalDateTime.now().plusHours(1);

            assertDoesNotThrow(() -> {
                contentService.schedulePublish(1L, scheduledTime, Arrays.asList(1, 2));
            });
        }

        @Test
        @DisplayName("取消定时发布")
        void shouldCancelScheduledPublish() {
            testContent.setScheduledTime(LocalDateTime.now().plusHours(1));
            when(contentMapper.selectById(1L)).thenReturn(testContent);
            when(contentMapper.updateById(any(Content.class))).thenReturn(1);

            contentService.cancelSchedulePublish(1L);

            verify(contentMapper).updateById(any(Content.class));
        }
    }

    @Nested
    @DisplayName("数据统计测试")
    class StatisticsTests {

        @Test
        @DisplayName("增加浏览量")
        void shouldIncrementViewCount() {
            when(contentMapper.incrementViewCount(1L)).thenReturn(1);

            assertDoesNotThrow(() -> {
                contentService.incrementViewCount(1L);
            });

            verify(contentMapper).incrementViewCount(1L);
        }
    }
}
