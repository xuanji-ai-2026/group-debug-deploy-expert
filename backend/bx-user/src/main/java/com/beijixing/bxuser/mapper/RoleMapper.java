package com.beijixing.bxuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.bxuser.entity.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    default Optional<Role> findByRoleKey(String roleKey) {
        return Optional.ofNullable(selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>()
                .eq("role_key", roleKey)
        ));
    }

    default boolean existsByRoleKey(String roleKey) {
        return selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Role>()
                .eq("role_key", roleKey)
        ) > 0;
    }
}
