package org.liangyou;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springdoc.core.models.GroupedOpenApi;

@SpringBootTest
@ActiveProfiles("dev")
class LiangyouDemoApplicationTests {

    @Autowired
    private Environment environment;

    @Autowired
    private List<GroupedOpenApi> groupedOpenApis;

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Test
    void contextLoads() {
    }

    @Test
    void defaultProfileIsDev() {
        Assertions.assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("dev"));
    }

    @Test
    void applicationNameIsConfigured() {
        Assertions.assertEquals("liangyoudemo", applicationName);
    }

    @Test
    void redisFallbackConfigIsConfigured() {
        Assertions.assertEquals("127.0.0.1", redisHost);
        Assertions.assertEquals(6379, redisPort);
        Assertions.assertEquals("", redisPassword);
    }

    @Test
    void openApiGroupsIncludeAlignedControllers() {
        Set<String> groupNames = groupedOpenApis.stream()
                .map(GroupedOpenApi::getGroup)
                .collect(Collectors.toSet());

        Assertions.assertTrue(groupNames.containsAll(Set.of(
                "Auth",
                "User",
                "Role",
                "Menu",
                "Purchase",
                "Sale",
                "Inventory",
                "Statistics"
        )));
    }
}
