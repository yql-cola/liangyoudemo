package org.liangyou.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "分页响应")
public class PageResponse<T> {

    @Schema(description = "当前页数据列表")
    private final List<T> list;
    @Schema(description = "总记录数", example = "28")
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
