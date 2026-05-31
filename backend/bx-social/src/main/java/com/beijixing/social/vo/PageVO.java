package com.beijixing.social.vo;

import java.util.List;

public class PageVO<T> {
    private Long total;
    private Long pageNum;
    private Long pageSize;
    private Long pages;
    private List<T> records;

    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Long getPageNum() { return pageNum; }
    public void setPageNum(Long pageNum) { this.pageNum = pageNum; }
    public Long getPageSize() { return pageSize; }
    public void setPageSize(Long pageSize) { this.pageSize = pageSize; }
    public Long getPages() { return pages; }
    public void setPages(Long pages) { this.pages = pages; }
    public List<T> getRecords() { return records; }
    public void setRecords(List<T> records) { this.records = records; }

    public static <T> PageVO<T> of(Long total, Long pageNum, Long pageSize, List<T> records) {
        PageVO<T> page = new PageVO<>();
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setPages((total + pageSize - 1) / pageSize);
        page.setRecords(records);
        return page;
    }
}
