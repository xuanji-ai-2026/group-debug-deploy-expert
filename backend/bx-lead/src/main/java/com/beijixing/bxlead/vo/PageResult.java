package com.beijixing.bxlead.vo;

import lombok.Data;
import java.util.List;

/**
 * 分页结果
 * @author 朱怡
 * @since 1.0.0
 */
@Data
public class PageResult<T> {
    
    /** 当前页 */
    private Integer pageNum;
    
    /** 每页大小 */
    private Integer pageSize;
    
    /** 总记录数 */
    private Long total;
    
    /** 总页数 */
    private Integer pages;
    
    /** 数据列表 */
    private List<T> list;
    
    public static <T> PageResult<T> of(Integer pageNum, Integer pageSize, Long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages((int) Math.ceil((double) total / pageSize));
        result.setList(list);
        return result;
    }
}