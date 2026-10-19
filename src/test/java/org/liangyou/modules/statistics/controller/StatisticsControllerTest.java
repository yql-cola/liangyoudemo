package org.liangyou.modules.statistics.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.service.StatisticsService;
import org.liangyou.modules.statistics.vo.AmountSummaryResponse;
import org.liangyou.modules.statistics.vo.DailyInboundOutboundResponse;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerTest {

    @Test
    void monthlyInboundOutboundEndpointExists() throws Exception {
        StatisticsService statisticsService = new StatisticsService(null) {
            @Override
            public List<AmountSummaryResponse> monthlyInboundOutbound(StatisticsQueryRequest request) {
                return List.of();
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/statistics/monthly/inbound-outbound"))
                .andExpect(status().isOk());
    }

    @Test
    void oldMonthlyAmountSummaryRouteDoesNotInvokeService() throws Exception {
        boolean[] called = new boolean[1];
        StatisticsService statisticsService = new StatisticsService(null) {
            @Override
            public List<AmountSummaryResponse> monthlyInboundOutbound(StatisticsQueryRequest request) {
                called[0] = true;
                return List.of();
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/statistics/monthly/amount-summary"))
                .andExpect(status().is4xxClientError());
        org.junit.jupiter.api.Assertions.assertFalse(called[0]);
    }

    @Test
    void dailyStatisticsEndpointExists() throws Exception {
        StatisticsService statisticsService = new StatisticsService(null) {
            @Override
            public List<DailyInboundOutboundResponse> dailyInboundOutbound(StatisticsQueryRequest request) {
                return List.of();
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/statistics/daily/inbound-outbound"))
                .andExpect(status().isOk());
    }

    @Test
    void amountByCategoryEndpointExists() throws Exception {
        StatisticsService statisticsService = new StatisticsService(null) {
            @Override
            public List<AmountSummaryResponse> amountByCategory(StatisticsQueryRequest request) {
                return List.of();
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/v1/statistics/amount-by-category"))
                .andExpect(status().isOk());
    }

    @Test
    void scenarioStatisticsEndpointsExist() throws Exception {
        StatisticsService statisticsService = new StatisticsService(null) {
            @Override public ScenarioStatisticsResponse dashboard(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse trends(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse saleProducts(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse purchaseProducts(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse inventoryStructure(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse customerRanks(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse supplierRanks(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
            @Override public ScenarioStatisticsResponse categoryShares(StatisticsQueryRequest request) { return new ScenarioStatisticsResponse(); }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        for (String path : List.of(
                "/api/v1/statistics/dashboard",
                "/api/v1/statistics/trends",
                "/api/v1/statistics/sale-products",
                "/api/v1/statistics/purchase-products",
                "/api/v1/statistics/inventory-structure",
                "/api/v1/statistics/customer-ranks",
                "/api/v1/statistics/supplier-ranks",
                "/api/v1/statistics/category-shares")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void scenarioStatisticsExportEndpointsExist() throws Exception {
        StatisticsService statisticsService = new StatisticsService(null) {
            @Override public byte[] exportScenario(String scenario, StatisticsQueryRequest request) {
                org.junit.jupiter.api.Assertions.assertEquals("2026-10-01", request.getDateFrom().toString());
                return new org.liangyou.modules.basic.service.ExcelExportService().export(scenario,
                        List.of(java.util.Map.of("名称", "大米", "金额", 100)));
            }
        };
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new StatisticsController(statisticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        for (String path : List.of(
                "/api/v1/statistics/dashboard/export",
                "/api/v1/statistics/trends/export",
                "/api/v1/statistics/sale-products/export",
                "/api/v1/statistics/purchase-products/export",
                "/api/v1/statistics/inventory-structure/export",
                "/api/v1/statistics/customer-ranks/export",
                "/api/v1/statistics/supplier-ranks/export",
                "/api/v1/statistics/category-shares/export")) {
            mockMvc.perform(get(path).param("dateFrom", "2026-10-01"))
                    .andExpect(status().isOk());
        }
    }
}
