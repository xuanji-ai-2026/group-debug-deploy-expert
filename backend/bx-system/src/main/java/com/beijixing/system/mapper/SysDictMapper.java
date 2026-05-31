package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysDict;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 字典 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysDictMapper extends BaseMapper<SysDict> {

    /**
     * 根据字典编码查询
     *
     * @param dictCode 字典编码
     * @return SysDict
     */
    SysDict selectByDictCode(@Param("dictCode") String dictCode);
}
