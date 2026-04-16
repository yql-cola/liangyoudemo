package org.liangyou.modules.statistics.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.common.web.GlobalExceptionHandler;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.service.StatisticsService;
import org.liangyou.modules.statistics.vo.AmountSummaryResponse;
import org.liangyou.modules.statistics.vo.DailyInboundOutboundResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatisticsControllerTest {

    @Test
    void monthlyInboundOutboundEndpointExists() throws Exception {
        StatisticsService statisticsService = new StatisticsService(null, null, null, null, null, null, null) {
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
        StatisticsService statisticsService = new StatisticsService(null, null, null, null, null, null, null) {
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
        StatisticsService statisticsService = new StatisticsService(null, null, null, null, null, null, null) {
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
        StatisticsService statisticsService = new StatisticsService(null, null, null, null, null, null, null) {
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
}
