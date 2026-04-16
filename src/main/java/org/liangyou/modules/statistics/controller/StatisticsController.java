package org.liangyou.modules.statistics.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.liangyou.common.api.ApiResponse;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.service.StatisticsService;
import org.liangyou.modules.statistics.vo.AmountSummaryResponse;
import org.liangyou.modules.statistics.vo.DailyInboundOutboundResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Statistics")
@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/daily/inbound-outbound")
    @Operation(summary = "按日查询进出货统计", description = "优先读取日汇总表，无数据时回退实时聚合。权限点：statistics:daily:view")
    public ApiResponse<List<DailyInboundOutboundResponse>> dailyInboundOutbound(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.dailyInboundOutbound(request));
    }

    @GetMapping("/monthly/inbound-outbound")
    @Operation(summary = "按月查询进出货统计", description = "返回月维度金额与数量汇总。权限点：statistics:monthly:view")
    public ApiResponse<List<AmountSummaryResponse>> monthlyInboundOutbound(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.monthlyInboundOutbound(request));
    }

    @GetMapping("/amount-by-customer")
    @Operation(summary = "按客户统计销售金额", description = "按客户聚合销售金额和数量。权限点：statistics:customer:view")
    public ApiResponse<List<AmountSummaryResponse>> amountByCustomer(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.amountByCustomer(request));
    }

    @GetMapping("/amount-by-supplier")
    @Operation(summary = "按供应商统计采购金额", description = "按供应商聚合采购金额和数量。权限点：statistics:supplier:view")
    public ApiResponse<List<AmountSummaryResponse>> amountBySupplier(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.amountBySupplier(request));
    }

    @GetMapping("/amount-by-category")
    @Operation(summary = "按分类统计金额", description = "按商品分类聚合采购或销售金额与数量。权限点：statistics:category:view")
    public ApiResponse<List<AmountSummaryResponse>> amountByCategory(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.amountByCategory(request));
    }
}
