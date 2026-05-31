package com.beijixing.social.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.beijixing.social.entity.SocialPlatform;
import com.beijixing.social.mapper.PlatformMapper;
import com.beijixing.social.vo.PlatformVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

/**
 * 平台服务
 */
@Service
@RequiredArgsConstructor
public class PlatformService extends ServiceImpl<PlatformMapper, SocialPlatform> {

    /** 初始化平台数据 */
    public void initPlatforms() {
        List<SocialPlatform> platforms = Arrays.asList(
            createPlatform("DOUYIN", "抖音", "https://open.douyin.com/oauth/authorize", 1),
            createPlatform("XIAOHONGSHU", "小红书", "https://developers.xiaohongshu.com/oauth2/authorize", 2),
            createPlatform("VIDEO", "视频号", "https://open.weixin.qq.com/connect/oauth2/authorize", 3),
            createPlatform("KUAISHOU", "快手", "https://open.kuaishou.com/oauth/authorize", 4),
            createPlatform("BILIBILI", "B站", "https://api.bilibili.com/v1/oauth2/authorize", 5),
            createPlatform("WEIBO", "微博", "https://api.weibo.com/oauth2/authorize", 6)
        );
        platforms.forEach(p -> {
            if (count(new LambdaQueryWrapper<SocialPlatform>().eq(SocialPlatform::getPlatformCode, p.getPlatformCode())) == 0) {
                save(p);
            }
        });
    }

    private SocialPlatform createPlatform(String code, String name, String authUrl, int order) {
        SocialPlatform p = new SocialPlatform();
        p.setPlatformCode(code);
        p.setPlatformName(name);
        p.setAuthorizeUrl(authUrl);
        p.setEnabled(1);
        p.setSortOrder(order);
        return p;
    }

    /** 获取启用的平台列表 */
    public List<PlatformVO> listEnabledPlatforms() {
        LambdaQueryWrapper<SocialPlatform> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SocialPlatform::getEnabled, 1).orderByAsc(SocialPlatform::getSortOrder);
        return list(wrapper).stream().map(this::toVO).toList();
    }

    /** 获取所有平台列表 */
    public List<PlatformVO> listAllPlatforms() {
        return list().stream().map(this::toVO).toList();
    }

    /** 启用/禁用平台 */
    public boolean togglePlatform(Long id, boolean enabled) {
        SocialPlatform platform = getById(id);
        if (platform == null) return false;
        platform.setEnabled(enabled ? 1 : 0);
        return updateById(platform);
    }

    private PlatformVO toVO(SocialPlatform p) {
        PlatformVO vo = new PlatformVO();
        vo.setId(p.getId());
        vo.setPlatformCode(p.getPlatformCode());
        vo.setPlatformName(p.getPlatformName());
        vo.setIconUrl(p.getIconUrl());
        vo.setAuthorizeUrl(p.getAuthorizeUrl());
        vo.setEnabled(p.getEnabled());
        return vo;
    }
}
