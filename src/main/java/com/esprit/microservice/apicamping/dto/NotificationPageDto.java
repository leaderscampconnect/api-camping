package com.esprit.microservice.apicamping.dto;

import java.util.List;

public class NotificationPageDto {
    private List<NotificationDto> data;
    private Pagination pagination;

    public NotificationPageDto() {}

    public List<NotificationDto> getData() { return data; }
    public void setData(List<NotificationDto> data) { this.data = data; }

    public Pagination getPagination() { return pagination; }
    public void setPagination(Pagination pagination) { this.pagination = pagination; }

    public static class Pagination {
        private int page;
        private int limit;
        private int total;
        private int pages;

        public Pagination() {}

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public int getPages() { return pages; }
        public void setPages(int pages) { this.pages = pages; }
    }
}
