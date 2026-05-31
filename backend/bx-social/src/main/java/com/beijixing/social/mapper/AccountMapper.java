package com.beijixing.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.social.entity.SocialAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 社媒账号Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<SocialAccount> {

    /**
     * 查询即将过期的账号
     */
    List<SocialAccount> selectExpiringAccounts(@Param("days") int days);

    /**
     * 查询异常账号
     */
    List<SocialAccount> selectAbnormalAccounts();

    /**
     * 更新最后活跃时间
     */
    int updateLastActiveTime(@Param("accountId") Long accountId);
}
