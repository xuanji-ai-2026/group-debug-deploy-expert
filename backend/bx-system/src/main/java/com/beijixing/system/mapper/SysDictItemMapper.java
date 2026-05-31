package com.beijixing.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.beijixing.system.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典项 Mapper 接口
 *
 * @author bx-system
 */
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {

    /**
     * 根据字典ID查询所有字典项
     *
     * @param dictId 字典ID
     * @return 字典项列表
     */
    List<SysDictItem> selectByDictId(@Param("dictId") Long dictId);

    /**
     * 根据字典ID查询已启用的字典项
     *
     * @param dictId 字典ID
     * @return 字典项列表
     */
    List<SysDictItem> selectEnabledByDictId(@Param("dictId") Long dictId);
}
