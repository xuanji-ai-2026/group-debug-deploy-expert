package com.beijixing.bxuser.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.beijixing.bxuser.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    default Optional<User> findByPhone(String phone) {
        return Optional.ofNullable(selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("phone", phone)
        ));
    }

    default Optional<User> findByEmail(String email) {
        return Optional.ofNullable(selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("email", email)
        ));
    }

    default boolean existsByPhone(String phone) {
        return selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("phone", phone)
        ) > 0;
    }

    default boolean existsByEmail(String email) {
        return selectCount(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                .eq("email", email)
        ) > 0;
    }

    IPage<User> findByRoleType(IPage<User> page, @Param("roleType") String roleType);

    IPage<User> findByStatus(IPage<User> page, @Param("status") Integer status);

    long countByStatus(@Param("status") Integer status);

    IPage<User> searchByKeyword(IPage<User> page, @Param("keyword") String keyword);
}
