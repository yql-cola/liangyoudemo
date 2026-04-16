package org.liangyou.common.api;

import java.util.List;

public class PageResponse<T> {

    private final List<T> list;
    private final long total;

    public PageResponse(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }
}
