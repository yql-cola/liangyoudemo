package org.liangyou;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class LiangyouDemoApplicationTests {

    @Autowired
    private Environment environment;

    @Value("${spring.application.name}")
    private String applicationName;

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
}
