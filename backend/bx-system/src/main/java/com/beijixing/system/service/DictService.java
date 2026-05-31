package com.beijixing.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysDict;
import com.beijixing.system.entity.SysDictItem;

import java.util.List;
import java.util.Map;

/**
 * 字典管理服务接口
 *
 * 功能：SM-002 字典管理（数据字典、字典项）
 *
 * @author bx-system
 */
public interface DictService {

    // ==================== 字典管理 ====================

    /**
     * 分页查询字典列表
     *
     * @param page 页码
     * @param size 每页数量
     * @param dictCode 字典编码（模糊搜索）
     * @param dictName 字典名称（模糊搜索）
     * @param status 状态
     * @return 分页结果
     */
    Page<SysDict> pageDicts(Integer page, Integer size, String dictCode, String dictName, Integer status);

    /**
     * 获取字典详情
     *
     * @param id 字典ID
     * @return 字典实体
     */
    SysDict getDictById(Long id);

    /**
     * 根据字典编码获取字典
     *
     * @param dictCode 字典编码
     * @return 字典实体
     */
    SysDict getDictByCode(String dictCode);

    /**
     * 创建字典
     *
     * @param dict 字典实体
     * @return 字典ID
     */
    Long createDict(SysDict dict);

    /**
     * 更新字典
     *
     * @param id 字典ID
     * @param dict 字典实体
     */
    void updateDict(Long id, SysDict dict);

    /**
     * 删除字典（同时删除所有字典项）
     *
     * @param id 字典ID
     */
    void deleteDict(Long id);

    /**
     * 更新字典状态
     *
     * @param id 字典ID
     * @param status 状态（0-禁用, 1-启用）
     */
    void updateDictStatus(Long id, Integer status);

    // ==================== 字典项管理 ====================

    /**
     * 获取字典的所有字典项
     *
     * @param dictId 字典ID
     * @return 字典项列表
     */
    List<SysDictItem> listItemsByDictId(Long dictId);

    /**
     * 获取字典的所有启用字典项
     *
     * @param dictId 字典ID
     * @return 字典项列表
     */
    List<SysDictItem> listEnabledItemsByDictId(Long dictId);

    /**
     * 根据字典编码获取所有启用字典项
     *
     * @param dictCode 字典编码
     * @return 字典项列表
     */
    List<SysDictItem> listItemsByDictCode(String dictCode);

    /**
     * 获取字典项Map（用于快速查找）
     *
     * @param dictCode 字典编码
     * @return Map（itemValue -> itemLabel）
     */
    Map<String, String> getItemLabelMap(String dictCode);

    /**
     * 创建字典项
     *
     * @param item 字典项实体
     * @return 字典项ID
     */
    Long createItem(SysDictItem item);

    /**
     * 批量创建字典项
     *
     * @param items 字典项列表
     */
    void batchCreateItems(List<SysDictItem> items);

    /**
     * 更新字典项
     *
     * @param id 字典项ID
     * @param item 字典项实体
     */
    void updateItem(Long id, SysDictItem item);

    /**
     * 删除字典项
     *
     * @param id 字典项ID
     */
    void deleteItem(Long id);

    /**
     * 更新字典项状态
     *
     * @param id 字典项ID
     * @param status 状态（0-禁用, 1-启用）
     */
    void updateItemStatus(Long id, Integer status);

    /**
     * 根据字典ID删除所有字典项
     *
     * @param dictId 字典ID
     */
    void deleteItemsByDictId(Long dictId);
}
