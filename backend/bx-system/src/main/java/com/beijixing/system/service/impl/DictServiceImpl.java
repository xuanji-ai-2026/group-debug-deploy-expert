package com.beijixing.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.beijixing.system.entity.SysDict;
import com.beijixing.system.entity.SysDictItem;
import com.beijixing.system.mapper.SysDictItemMapper;
import com.beijixing.system.mapper.SysDictMapper;
import com.beijixing.system.service.DictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 字典管理服务实现
 *
 * @author bx-system
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    private final SysDictMapper dictMapper;
    private final SysDictItemMapper dictItemMapper;

    // ==================== 字典管理 ====================

    @Override
    public Page<SysDict> pageDicts(Integer page, Integer size, String dictCode, String dictName, Integer status) {
        Page<SysDict> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysDict> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dictCode)) {
            wrapper.like(SysDict::getDictCode, dictCode);
        }
        if (StringUtils.hasText(dictName)) {
            wrapper.like(SysDict::getDictName, dictName);
        }
        if (status != null) {
            wrapper.eq(SysDict::getStatus, status);
        }
        wrapper.orderByDesc(SysDict::getId);

        Page<SysDict> result = dictMapper.selectPage(pageParam, wrapper);
        log.debug("分页查询字典列表，结果：{}条", result.getTotal());
        return result;
    }

    @Override
    public SysDict getDictById(Long id) {
        return dictMapper.selectById(id);
    }

    @Override
    public SysDict getDictByCode(String dictCode) {
        return dictMapper.selectByDictCode(dictCode);
    }

    @Override
    public Long createDict(SysDict dict) {
        // 检查编码唯一性
        SysDict existing = dictMapper.selectByDictCode(dict.getDictCode());
        if (existing != null) {
            throw new IllegalArgumentException("字典编码已存在：" + dict.getDictCode());
        }
        if (dict.getStatus() == null) {
            dict.setStatus(1);
        }
        dictMapper.insert(dict);
        log.info("创建字典：{} - {}", dict.getDictCode(), dict.getDictName());
        return dict.getId();
    }

    @Override
    public void updateDict(Long id, SysDict dict) {
        SysDict existing = dictMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("字典不存在：" + id);
        }
        dict.setId(id);
        dictMapper.updateById(dict);
        log.info("更新字典：id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDict(Long id) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new IllegalArgumentException("字典不存在：" + id);
        }
        // 先删除所有字典项
        dictItemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, id));
        // 再删除字典
        dictMapper.deleteById(id);
        log.info("删除字典及其所有字典项：id={}, code={}", id, dict.getDictCode());
    }

    @Override
    public void updateDictStatus(Long id, Integer status) {
        SysDict dict = dictMapper.selectById(id);
        if (dict == null) {
            throw new IllegalArgumentException("字典不存在：" + id);
        }
        dict.setStatus(status);
        dictMapper.updateById(dict);
        log.info("更新字典状态：id={}, status={}", id, status);
    }

    // ==================== 字典项管理 ====================

    @Override
    public List<SysDictItem> listItemsByDictId(Long dictId) {
        return dictItemMapper.selectByDictId(dictId);
    }

    @Override
    public List<SysDictItem> listEnabledItemsByDictId(Long dictId) {
        return dictItemMapper.selectEnabledByDictId(dictId);
    }

    @Override
    public List<SysDictItem> listItemsByDictCode(String dictCode) {
        SysDict dict = dictMapper.selectByDictCode(dictCode);
        if (dict == null) {
            throw new IllegalArgumentException("字典不存在：" + dictCode);
        }
        return dictItemMapper.selectEnabledByDictId(dict.getId());
    }

    @Override
    public Map<String, String> getItemLabelMap(String dictCode) {
        List<SysDictItem> items = listItemsByDictCode(dictCode);
        return items.stream().collect(Collectors.toMap(SysDictItem::getItemValue, SysDictItem::getItemLabel));
    }

    @Override
    public Long createItem(SysDictItem item) {
        // 设置字典类型
        SysDict dict = dictMapper.selectById(item.getDictId());
        if (dict == null) {
            throw new IllegalArgumentException("字典不存在：" + item.getDictId());
        }
        if (item.getItemType() == null) {
            item.setItemType(dict.getDictType());
        }
        if (item.getStatus() == null) {
            item.setStatus(1);
        }
        if (item.getSortOrder() == null) {
            item.setSortOrder(0);
        }
        if (item.getIsDefault() == null) {
            item.setIsDefault(0);
        }
        dictItemMapper.insert(item);
        log.info("创建字典项：dictId={}, value={}", item.getDictId(), item.getItemValue());
        return item.getId();
    }

    @Override
    public void batchCreateItems(List<SysDictItem> items) {
        for (SysDictItem item : items) {
            createItem(item);
        }
        log.info("批量创建字典项，数量：{}", items.size());
    }

    @Override
    public void updateItem(Long id, SysDictItem item) {
        SysDictItem existing = dictItemMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("字典项不存在：" + id);
        }
        item.setId(id);
        dictItemMapper.updateById(item);
        log.info("更新字典项：id={}", id);
    }

    @Override
    public void deleteItem(Long id) {
        dictItemMapper.deleteById(id);
        log.info("删除字典项：id={}", id);
    }

    @Override
    public void updateItemStatus(Long id, Integer status) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("字典项不存在：" + id);
        }
        item.setStatus(status);
        dictItemMapper.updateById(item);
        log.info("更新字典项状态：id={}, status={}", id, status);
    }

    @Override
    public void deleteItemsByDictId(Long dictId) {
        dictItemMapper.delete(new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictId, dictId));
        log.info("删除字典的所有字典项：dictId={}", dictId);
    }
}
