package com.beijixing.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.social.entity.SocialPlatform;
import org.apache.ibatis.annotations.Mapper;

/**
 * 社交平台Mapper
 */
@Mapper
public interface PlatformMapper extends BaseMapper<SocialPlatform> {
}
