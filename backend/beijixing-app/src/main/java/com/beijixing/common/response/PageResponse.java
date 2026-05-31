package com.beijixing.common.response;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class PageResponse<T> extends ApiResponse<List<T>> {

    private long total;
    private long page;
    private long size;
    private long pages;

    public PageResponse() {
        super();
    }

    public static <T> PageResponse<T> of(Page<T> pageResult) {
        PageResponse<T> response = new PageResponse<>();
        response.setCode("000000");
        response.setMessage("操作成功");
        response.setData(pageResult.getRecords());
        response.setTotal(pageResult.getTotal());
        response.setPage(pageResult.getCurrent());
        response.setSize(pageResult.getSize());
        response.setPages(pageResult.getPages());
        return response;
    }

    public static <T> PageResponse<T> of(List<T> list, long total, long page, long size) {
        PageResponse<T> response = new PageResponse<>();
        response.setCode("000000");
        response.setMessage("操作成功");
        response.setData(list);
        response.setTotal(total);
        response.setPage(page);
        response.setSize(size);
        response.setPages((total + size - 1) / size);
        return response;
    }
}
