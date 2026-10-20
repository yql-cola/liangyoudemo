package org.liangyou.modules.statistics.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.vo.ScenarioStatisticsResponse.Row;
import java.util.List;

@Mapper
public interface ScenarioStatisticsMapper {
    List<Row> business(@Param("q") StatisticsQueryRequest request, @Param("source") String source,
                       @Param("dimension") String dimension);
    List<Row> inventory(@Param("q") StatisticsQueryRequest request, @Param("dimension") String dimension);
}
