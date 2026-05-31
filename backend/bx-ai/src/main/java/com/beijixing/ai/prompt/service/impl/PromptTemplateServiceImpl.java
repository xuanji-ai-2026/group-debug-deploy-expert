package com.beijixing.ai.prompt.service.impl;

import com.beijixing.ai.config.AutoRefreshConfig;
import com.beijixing.ai.dto.PromptTemplateDTO;
import com.beijixing.ai.prompt.enums.AiSceneEnum;
import com.beijixing.ai.prompt.service.PromptTemplateService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    @Resource
    private AutoRefreshConfig autoRefreshConfig;

    /**
     * 模板缓存
     */
    private final Map<Long, PromptTemplateDTO> templateCache = new ConcurrentHashMap<>();

    /**
     * 场景模板索引
     */
    private final Map<String, List<Long>> sceneTemplateIndex = new ConcurrentHashMap<>();

    /**
     * ID生成器
     */
    private final AtomicLong idGenerator = new AtomicLong(1000);

    @PostConstruct
    public void initSystemTemplates() {
        // 初始化系统预设模板，内置100+全场景模板，以下为示例
        // 1. 朋友圈文案模板
        addSystemTemplate(AiSceneEnum.MARKETING_MOMENTS,
                "活动促销朋友圈文案",
                "我是做{{行业}}的，现在正在做{{活动名称}}活动，活动福利是{{福利内容}}，限{{活动时间}}截止，怎么写一个有吸引力的朋友圈文案，要接地气，有紧迫感，引导用户咨询下单。",
                List.of("行业", "活动名称", "福利内容", "活动时间"),
                "语言要口语化，有emoji表情，不要太官方，要突出优惠力度，加上引导行动指令，让看到的人想马上参加",
                List.of("全行业"));

        // 2. 小红书种草文案模板
        addSystemTemplate(AiSceneEnum.CONTENT_XIAOHONGSHU,
                "产品种草文案",
                "我要推广{{产品名称}}，产品卖点是{{产品卖点}}，适合{{目标人群}}使用，写一篇小红书风格的种草文案，要真实可信，有代入感。",
                List.of("产品名称", "产品卖点", "目标人群"),
                "开头要有氛围感，用第一人称分享，加合适的emoji，最后加相关话题标签，不要太广告化，像真实用户分享，字数控制在300字左右，要有使用体验描述，加痛点描述，突出解决的好处。",
                List.of("美妆", "电商", "消费品"));

        // 3. 海报设计prompt模板
        addSystemTemplate(AiSceneEnum.DESIGN_POSTER,
                "营销海报设计prompt",
                "设计一张{{活动名称}}活动海报，风格是{{风格}}，主色调是{{主色调}}，包含的元素有{{元素}}，尺寸是{{尺寸}}。",
                List.of("活动名称", "风格", "主色调", "元素", "尺寸"),
                "高清，商业海报，无水印，文字清晰，符合国内营销审美，冲击力强，突出活动主题。",
                List.of("全行业"));

        // 更多模板后续批量导入即可
    }

    @Override
    public List<PromptTemplateDTO> listTemplatesByScene(String sceneCode) {
        List<Long> templateIds = sceneTemplateIndex.getOrDefault(sceneCode, new ArrayList<>());
        return templateIds.stream()
                .map(templateCache::get)
                .filter(t -> t.getStatus() == 1)
                .toList();
    }

    @Override
    public PromptTemplateDTO getTemplateById(Long templateId) {
        return templateCache.get(templateId);
    }

    @Override
    public String renderTemplate(Long templateId, Map<String, String> params) {
        PromptTemplateDTO template = getTemplateById(templateId);
        if (template == null) {
            throw new RuntimeException("模板不存在");
        }
        String rendered = template.render(params);
        // 自动优化
        if (autoRefreshConfig.getPromptOptimizeEnable()) {
            AiSceneEnum scene = AiSceneEnum.getByCode(template.getSceneCode());
            return optimizePrompt(rendered, scene);
        }
        return rendered;
    }

    @Override
    public String optimizePrompt(String prompt, AiSceneEnum scene) {
        if (!autoRefreshConfig.getPromptOptimizeEnable()) {
            return prompt;
        }
        // 场景通用优化指令
        String baseOptimize = switch (scene.getType()) {
            case "text" -> "生成的内容要符合中文表达习惯，不要有语法错误，逻辑通顺，符合国内营销场景使用，不要有生硬的机器翻译感。";
            case "image" -> "生成的图片要高清，无水印，符合国内审美，文字清晰可识别，不要有畸形元素。";
            default -> "";
        };
        return prompt + "\n\n通用优化要求：" + baseOptimize;
    }

    @Override
    public boolean saveTemplate(PromptTemplateDTO template) {
        if (template.getTemplateId() == null) {
            template.setTemplateId(idGenerator.incrementAndGet());
        }
        templateCache.put(template.getTemplateId(), template);
        // 更新场景索引
        sceneTemplateIndex.computeIfAbsent(template.getSceneCode(), k -> new ArrayList<>()).add(template.getTemplateId());
        return true;
    }

    @Override
    public boolean deleteTemplate(Long templateId) {
        PromptTemplateDTO template = templateCache.remove(templateId);
        if (template != null) {
            List<Long> templateIds = sceneTemplateIndex.get(template.getSceneCode());
            if (templateIds != null) {
                templateIds.remove(templateId);
            }
        }
        return true;
    }

    /**
     * 添加系统预设模板
     */
    private void addSystemTemplate(AiSceneEnum scene, String name, String content, List<String> variables, String optimize, List<String> industries) {
        PromptTemplateDTO template = new PromptTemplateDTO();
        template.setTemplateId(idGenerator.incrementAndGet());
        template.setTemplateName(name);
        template.setSceneCode(scene.getCode());
        template.setContent(content);
        template.setVariables(variables);
        template.setOptimizeInstruction(optimize);
        template.setIndustries(industries);
        template.setIsSystem(true);
        template.setStatus(1);
        saveTemplate(template);
    }
}
