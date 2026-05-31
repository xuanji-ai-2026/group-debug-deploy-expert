package com.beijixing.content.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 通用分页结果VO
 * @author 胡云 (EMP-CONTENT-001)
 */
@Data
@Schema(description = "分页结果")
public class PageResultVO<T> {

    @Schema(description = "当前页")
    private Integer pageNum;

    @Schema(description = "每页大小")
    private Integer pageSize;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "总页数")
    private Integer pages;

    @Schema(description = "数据列表")
    private java.util.List<T> list;

    public static <T> PageResultVO<T> of(Integer pageNum, Integer pageSize, Long total, java.util.List<T> list) {
        PageResultVO<T> result = new PageResultVO<>();
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setList(list);
        result.setPages((int) Math.ceil((double) total / pageSize));
        return result;
    }
}
