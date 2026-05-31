package com.beijixing.bxuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.bxuser.entity.RefreshToken;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    @Delete("DELETE FROM sys_refresh_token WHERE expires_at < #{now} OR is_revoked = 1")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
