package org.liangyou.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.swagger")
public class SwaggerGlobalHeaderProperties {

    private List<String> globalHeaders = new ArrayList<>();
}
