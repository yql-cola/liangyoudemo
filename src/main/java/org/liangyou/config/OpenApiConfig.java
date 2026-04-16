package org.liangyou.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String COMMON_DESCRIPTION = """
            米面粮油库存管理系统接口文档。

            鉴权说明：
            1. 点击右上角 Authorize
            2. 输入 Bearer Token，格式为：Bearer <JWT>
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

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Liangyou Demo API")
                        .version("v1.0.0")
                        .description(COMMON_DESCRIPTION)
                        .license(new License().name("Internal Use Only")))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components().addSecuritySchemes(
                        "BearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }

    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
                .group("Auth")
                .pathsToMatch("/api/v1/auth/**")
                .build();
    }

    @Bean
    public GroupedOpenApi userGroup() {
        return GroupedOpenApi.builder()
                .group("User")
                .pathsToMatch("/api/v1/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi purchaseGroup() {
        return GroupedOpenApi.builder()
                .group("Purchase")
                .pathsToMatch("/api/v1/purchase-ins/**")
                .build();
    }

    @Bean
    public GroupedOpenApi saleGroup() {
        return GroupedOpenApi.builder()
                .group("Sale")
                .pathsToMatch("/api/v1/sale-outs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi inventoryGroup() {
        return GroupedOpenApi.builder()
                .group("Inventory")
                .pathsToMatch("/api/v1/inventories/**")
                .build();
    }

    @Bean
    public GroupedOpenApi statisticsGroup() {
        return GroupedOpenApi.builder()
                .group("Statistics")
                .pathsToMatch("/api/v1/statistics/**")
                .build();
    }
}
