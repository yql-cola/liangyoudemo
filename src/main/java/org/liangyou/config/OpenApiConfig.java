package org.liangyou.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfig {

    private static final String COMMON_DESCRIPTION = """
            米面粮油库存管理系统接口文档。

            鉴权说明：
            1. 点击右上角 Authorize
            2. 在 Authorization 参数中输入完整值，格式为：Bearer <JWT>
            3. 已开放认证接口无需鉴权，其余接口默认按 BearerAuth 调试

            统一错误码：
            - 0：成功
            - 400：参数错误或业务异常
            - 401：未登录或 Token 无效
            - 403：无权限访问
            - 500：系统内部异常

            调试说明：
            - 开发环境通过 /doc.html 访问 Knife4j 页面
            - 列表接口默认使用 pageNum/pageSize 分页参数
            - 金额字段统一使用 decimal，日期字段按接口示例格式传值
            """;
    private final SwaggerGlobalHeaderProperties swaggerGlobalHeaderProperties;

    @Bean
    public OpenAPI openAPI() {
        Components components = new Components();
        components.addSecuritySchemes(
                "BearerAuth",
                new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization")
                        .description("请输入完整鉴权值：Bearer <JWT>")
        );

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("BearerAuth");
        for (String headerName : resolveGlobalHeaders()) {
            String schemeName = "GlobalHeader" + toAlphaNumericToken(headerName);
            components.addSecuritySchemes(
                    schemeName,
                    new SecurityScheme()
                            .type(SecurityScheme.Type.APIKEY)
                            .in(SecurityScheme.In.HEADER)
                            .name(headerName)
                            .description("全局请求头：" + headerName)
            );
            securityRequirement.addList(schemeName);
        }

        return new OpenAPI()
                .info(new Info()
                        .title("粮油管理系统 API（Liangyou Demo API）")
                        .version("v1.1.0")
                        .description(COMMON_DESCRIPTION)
                        .license(new License().name("Internal Use Only")))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    private Set<String> resolveGlobalHeaders() {
        Set<String> result = new LinkedHashSet<>();
        for (String value : swaggerGlobalHeaderProperties.getGlobalHeaders()) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            String headerName = value.trim();
            if ("authorization".equalsIgnoreCase(headerName)) {
                continue;
            }
            result.add(headerName);
        }
        return result;
    }

    private String toAlphaNumericToken(String value) {
        String[] parts = value.trim().toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
                .group("认证（Auth）")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userGroup() {
        return GroupedOpenApi.builder()
                .group("用户（User）")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi roleGroup() {
        return GroupedOpenApi.builder()
                .group("角色（Role）")
                .pathsToMatch("/api/v1/roles/**")
                .build();
    }

    @Bean
    public GroupedOpenApi menuGroup() {
        return GroupedOpenApi.builder()
                .group("菜单（Menu）")
                .pathsToMatch("/api/v1/menus/**")
                .build();
    }

    @Bean
    public GroupedOpenApi purchaseGroup() {
        return GroupedOpenApi.builder()
                .group("采购（Purchase）")
                .pathsToMatch("/api/v1/purchase-ins/**")
                .build();
    }

    @Bean
    public GroupedOpenApi saleGroup() {
        return GroupedOpenApi.builder()
                .group("销售（Sale）")
                .pathsToMatch("/api/v1/sale-outs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi inventoryGroup() {
        return GroupedOpenApi.builder()
                .group("库存（Inventory）")
                .pathsToMatch("/api/v1/inventories/**")
                .build();
    }

    @Bean
    public GroupedOpenApi basicDataGroup() {
        return GroupedOpenApi.builder()
                .group("基础资料（Basic）")
                .pathsToMatch("/api/v1/basic/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statisticsGroup() {
        return GroupedOpenApi.builder()
                .group("统计（Statistics）")
                .pathsToMatch("/api/v1/statistics/**")
                .build();
    }
}
