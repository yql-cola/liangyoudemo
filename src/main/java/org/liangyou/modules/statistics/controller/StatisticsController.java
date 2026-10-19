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
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/dashboard")
    @Operation(summary = "经营看板", description = "权限点：statistics:dashboard:view")
    public ApiResponse<ScenarioStatisticsResponse> dashboard(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.dashboard(request));
    }

    @GetMapping("/trends")
    @Operation(summary = "进销趋势分析", description = "权限点：statistics:trend:view")
    public ApiResponse<ScenarioStatisticsResponse> trends(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.trends(request));
    }

    @GetMapping("/sale-products")
    @Operation(summary = "商品销售分析", description = "权限点：statistics:sale-product:view")
    public ApiResponse<ScenarioStatisticsResponse> saleProducts(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.saleProducts(request));
    }

    @GetMapping("/purchase-products")
    @Operation(summary = "商品采购分析", description = "权限点：statistics:purchase-product:view")
    public ApiResponse<ScenarioStatisticsResponse> purchaseProducts(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.purchaseProducts(request));
    }

    @GetMapping("/inventory-structure")
    @Operation(summary = "库存结构分析", description = "权限点：statistics:inventory-structure:view")
    public ApiResponse<ScenarioStatisticsResponse> inventoryStructure(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.inventoryStructure(request));
    }

    @GetMapping("/customer-ranks")
    @Operation(summary = "客户销售排行", description = "权限点：statistics:customer-rank:view")
    public ApiResponse<ScenarioStatisticsResponse> customerRanks(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.customerRanks(request));
    }

    @GetMapping("/supplier-ranks")
    @Operation(summary = "供应商采购排行", description = "权限点：statistics:supplier-rank:view")
    public ApiResponse<ScenarioStatisticsResponse> supplierRanks(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.supplierRanks(request));
    }

    @GetMapping("/category-shares")
    @Operation(summary = "分类占比分析", description = "权限点：statistics:category-share:view")
    public ApiResponse<ScenarioStatisticsResponse> categoryShares(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.categoryShares(request));
    }

    @GetMapping({
            "/dashboard/export",
            "/trends/export",
            "/sale-products/export",
            "/purchase-products/export",
            "/inventory-structure/export",
            "/customer-ranks/export",
            "/supplier-ranks/export",
            "/category-shares/export"
    })
    @Operation(summary = "导出统计报表")
    public ResponseEntity<byte[]> export(jakarta.servlet.http.HttpServletRequest http, StatisticsQueryRequest request) {
        String scenario = http.getRequestURI().substring(http.getContextPath().length() + "/api/v1/statistics/".length());
        scenario = scenario.substring(0, scenario.length() - "/export".length());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"statistics.xlsx\"")
                .body(statisticsService.exportScenario(scenario, request));
    }

    @GetMapping("/daily/inbound-outbound")
    @Operation(summary = "按日查询进出货统计", description = "按有效业务单据实时聚合金额与基础数量。权限点：statistics:daily:view")
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
