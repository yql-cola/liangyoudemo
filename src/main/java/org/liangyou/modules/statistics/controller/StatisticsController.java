package org.liangyou.modules.statistics.controller;

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
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/daily/inbound-outbound")
    public ApiResponse<List<DailyInboundOutboundResponse>> dailyInboundOutbound(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.dailyInboundOutbound(request));
    }

    @GetMapping("/monthly/amount-summary")
    public ApiResponse<List<AmountSummaryResponse>> monthlyAmountSummary(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.monthlyAmountSummary(request));
    }

    @GetMapping("/amount-by-customer")
    public ApiResponse<List<AmountSummaryResponse>> amountByCustomer(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.amountByCustomer(request));
    }

    @GetMapping("/amount-by-supplier")
    public ApiResponse<List<AmountSummaryResponse>> amountBySupplier(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.amountBySupplier(request));
    }

    @GetMapping("/amount-by-category")
    public ApiResponse<List<AmountSummaryResponse>> amountByCategory(StatisticsQueryRequest request) {
        return ApiResponse.success(statisticsService.amountByCategory(request));
    }
}
