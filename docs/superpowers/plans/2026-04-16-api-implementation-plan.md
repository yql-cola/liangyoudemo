# API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real MySQL-backed user, purchase, sale, inventory, and statistics APIs on top of the existing Spring Boot skeleton.

**Architecture:** Keep the current single-module Spring Boot structure and add focused module code for entity, mapper, service, controller, DTO, and VO layers. Start with schema alignment and shared infrastructure, then implement user authorization, purchase and sale stock flows, inventory maintenance, and statistics queries in dependency order.

**Tech Stack:** Java 17, Spring Boot 3, MyBatis Plus, MySQL, Spring Validation, springdoc-openapi, JUnit 5, MockMvc

---

### Task 1: Align schema and add shared backend infrastructure

**Files:**
- Modify: `sql/grain_oil_ims_init.sql`
- Create: `src/main/java/org/liangyou/common/api/PageResponse.java`
- Create: `src/main/java/org/liangyou/common/exception/BusinessException.java`
- Create: `src/main/java/org/liangyou/common/web/GlobalExceptionHandler.java`
- Create: `src/main/java/org/liangyou/config/MybatisPlusConfig.java`
- Test: `src/test/java/org/liangyou/common/web/GlobalExceptionHandlerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.liangyou.common.web;

import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    @Test
    void businessExceptionReturnsStandardErrorBody() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new DemoController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/demo").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("demo-error"));
    }

    @RestController
    static class DemoController {
        @GetMapping("/demo")
        public void demo() {
            throw new BusinessException(400, "demo-error");
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: FAIL because `BusinessException` and `GlobalExceptionHandler` do not exist

- [ ] **Step 3: Write minimal implementation**

Add `sys_user_permission` and `deleted` columns required by logical-delete flows to `sql/grain_oil_ims_init.sql`.

Create:

```java
package org.liangyou.common.exception;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

```java
package org.liangyou.common.web;

import org.liangyou.common.api.ApiResponse;
import org.liangyou.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return new ApiResponse<>(ex.getCode(), ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException ex) {
        return new ApiResponse<>(400, "validation-error", null);
    }
}
```

```java
package org.liangyou.common.api;

import java.util.List;

public class PageResponse<T> {
    private final List<T> list;
    private final long total;

    public PageResponse(List<T> list, long total) {
        this.list = list;
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public long getTotal() {
        return total;
    }
}
```

Add a minimal `MybatisPlusConfig` bean to enable pagination.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add sql/grain_oil_ims_init.sql src/main/java/org/liangyou/common src/main/java/org/liangyou/config/MybatisPlusConfig.java src/test/java/org/liangyou/common/web/GlobalExceptionHandlerTest.java
git commit -m "feat: add shared api infrastructure"
```

### Task 2: Implement user and authorization APIs

**Files:**
- Create: `src/main/java/org/liangyou/modules/user/entity/SysUser.java`
- Create: `src/main/java/org/liangyou/modules/user/entity/SysRole.java`
- Create: `src/main/java/org/liangyou/modules/user/entity/SysPermission.java`
- Create: `src/main/java/org/liangyou/modules/user/entity/SysUserRole.java`
- Create: `src/main/java/org/liangyou/modules/user/entity/SysRolePermission.java`
- Create: `src/main/java/org/liangyou/modules/user/entity/SysUserPermission.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/UserCreateRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/UserUpdateRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/UserRoleAssignRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/UserPermissionAssignRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/RolePermissionAssignRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/dto/UserQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/user/vo/UserDetailResponse.java`
- Create: `src/main/java/org/liangyou/modules/user/vo/UserPermissionResponse.java`
- Create: `src/main/java/org/liangyou/modules/user/mapper/SysUserMapper.java`
- Create: `src/main/java/org/liangyou/modules/user/mapper/SysRoleMapper.java`
- Create: `src/main/java/org/liangyou/modules/user/mapper/SysPermissionMapper.java`
- Create: `src/main/java/org/liangyou/modules/user/mapper/SysUserRoleMapper.java`
- Create: `src/main/java/org/liangyou/modules/user/mapper/SysRolePermissionMapper.java`
- Create: `src/main/java/org/liangyou/modules/user/mapper/SysUserPermissionMapper.java`
- Create: `src/main/java/org/liangyou/modules/user/service/UserService.java`
- Modify: `src/main/java/org/liangyou/modules/user/controller/UserController.java`
- Test: `src/test/java/org/liangyou/modules/user/controller/UserControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.liangyou.modules.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.user.dto.UserCreateRequest;
import org.liangyou.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    void createUserReturnsCreatedUserId() throws Exception {
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(100L);

        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("warehouse01");
        request.setPassword("123456");
        request.setRealName("仓库A");
        request.setWarehouseId(1L);
        request.setStatus(1);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(100));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=UserControllerTest test`
Expected: FAIL because DTO and service do not exist and `UserController` has no endpoint

- [ ] **Step 3: Write minimal implementation**

Implement:
- user entities mapped to `sys_*` tables with MyBatis Plus annotations
- request/response DTOs
- `UserService` methods:
  - `createUser`
  - `updateUser`
  - `deleteUser`
  - `getUserDetail`
  - `queryUsers`
  - `assignRoles`
  - `assignUserPermissions`
  - `assignRolePermissions`
  - `getUserPermissions`
- `UserController` endpoints matching the `/api` design

For user permission merging:
- load role permissions by role ids
- load direct grants and revokes from `sys_user_permission`
- final permissions = role permissions + direct grants - direct revokes

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=UserControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/user src/test/java/org/liangyou/modules/user/controller/UserControllerTest.java
git commit -m "feat: implement user and authorization apis"
```

### Task 3: Implement purchase inbound APIs and stock-in flow

**Files:**
- Create: `src/main/java/org/liangyou/modules/purchase/entity/BizPurchaseIn.java`
- Create: `src/main/java/org/liangyou/modules/purchase/entity/BizPurchaseInItem.java`
- Create: `src/main/java/org/liangyou/modules/purchase/dto/PurchaseInCreateRequest.java`
- Create: `src/main/java/org/liangyou/modules/purchase/dto/PurchaseInItemRequest.java`
- Create: `src/main/java/org/liangyou/modules/purchase/dto/PurchaseInQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/purchase/vo/PurchaseInDetailResponse.java`
- Create: `src/main/java/org/liangyou/modules/purchase/mapper/BizPurchaseInMapper.java`
- Create: `src/main/java/org/liangyou/modules/purchase/mapper/BizPurchaseInItemMapper.java`
- Create: `src/main/java/org/liangyou/modules/inventory/entity/InvStock.java`
- Create: `src/main/java/org/liangyou/modules/inventory/entity/InvBatch.java`
- Create: `src/main/java/org/liangyou/modules/inventory/entity/InvStockFlow.java`
- Create: `src/main/java/org/liangyou/modules/inventory/mapper/InvStockMapper.java`
- Create: `src/main/java/org/liangyou/modules/inventory/mapper/InvBatchMapper.java`
- Create: `src/main/java/org/liangyou/modules/inventory/mapper/InvStockFlowMapper.java`
- Create: `src/main/java/org/liangyou/modules/purchase/service/PurchaseInService.java`
- Modify: `src/main/java/org/liangyou/modules/purchase/controller/PurchaseInController.java`
- Test: `src/test/java/org/liangyou/modules/purchase/controller/PurchaseInControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.liangyou.modules.purchase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.purchase.dto.PurchaseInCreateRequest;
import org.liangyou.modules.purchase.dto.PurchaseInItemRequest;
import org.liangyou.modules.purchase.service.PurchaseInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseInController.class)
@Import(SecurityConfig.class)
class PurchaseInControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PurchaseInService purchaseInService;

    @Test
    void createPurchaseInReturnsDocumentId() throws Exception {
        when(purchaseInService.create(any(PurchaseInCreateRequest.class))).thenReturn(200L);

        PurchaseInItemRequest item = new PurchaseInItemRequest();
        item.setProductId(1L);
        item.setUnitId(1L);
        item.setQty(new BigDecimal("10"));
        item.setUnitPrice(new BigDecimal("100"));
        item.setAmount(new BigDecimal("1000"));
        item.setBatchNo("B001");
        item.setProductionDate(LocalDate.now());
        item.setExpiryDate(LocalDate.now().plusMonths(6));
        item.setInboundTime(LocalDateTime.now());

        PurchaseInCreateRequest request = new PurchaseInCreateRequest();
        request.setWarehouseId(1L);
        request.setSupplierId(1L);
        request.setBizDate(LocalDate.now());
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/purchase-ins")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(200));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=PurchaseInControllerTest test`
Expected: FAIL because purchase DTO/service do not exist and controller has no endpoint

- [ ] **Step 3: Write minimal implementation**

Implement purchase create/list/detail/update/delete with:
- order number generation like `PIyyyyMMddHHmmssSSS`
- insert into `biz_purchase_in` and `biz_purchase_in_item`
- upsert `inv_stock`
- insert `inv_batch`
- insert positive `inv_stock_flow`

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=PurchaseInControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/purchase src/main/java/org/liangyou/modules/inventory/entity src/main/java/org/liangyou/modules/inventory/mapper src/test/java/org/liangyou/modules/purchase/controller/PurchaseInControllerTest.java
git commit -m "feat: implement purchase inbound api"
```

### Task 4: Implement sale outbound APIs and FIFO stock-out flow

**Files:**
- Create: `src/main/java/org/liangyou/modules/sale/entity/BizSaleOut.java`
- Create: `src/main/java/org/liangyou/modules/sale/entity/BizSaleOutItem.java`
- Create: `src/main/java/org/liangyou/modules/inventory/entity/InvSaleBatchDeduction.java`
- Create: `src/main/java/org/liangyou/modules/sale/dto/SaleOutCreateRequest.java`
- Create: `src/main/java/org/liangyou/modules/sale/dto/SaleOutItemRequest.java`
- Create: `src/main/java/org/liangyou/modules/sale/dto/SaleOutQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/sale/vo/SaleOutDetailResponse.java`
- Create: `src/main/java/org/liangyou/modules/sale/mapper/BizSaleOutMapper.java`
- Create: `src/main/java/org/liangyou/modules/sale/mapper/BizSaleOutItemMapper.java`
- Create: `src/main/java/org/liangyou/modules/inventory/mapper/InvSaleBatchDeductionMapper.java`
- Create: `src/main/java/org/liangyou/modules/sale/service/SaleOutService.java`
- Modify: `src/main/java/org/liangyou/modules/sale/controller/SaleOutController.java`
- Test: `src/test/java/org/liangyou/modules/sale/controller/SaleOutControllerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package org.liangyou.modules.sale.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.sale.dto.SaleOutCreateRequest;
import org.liangyou.modules.sale.dto.SaleOutItemRequest;
import org.liangyou.modules.sale.service.SaleOutService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SaleOutController.class)
@Import(SecurityConfig.class)
class SaleOutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SaleOutService saleOutService;

    @Test
    void createSaleOutReturnsDocumentId() throws Exception {
        when(saleOutService.create(any(SaleOutCreateRequest.class))).thenReturn(300L);

        SaleOutItemRequest item = new SaleOutItemRequest();
        item.setProductId(1L);
        item.setUnitId(1L);
        item.setQty(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("150"));
        item.setAmount(new BigDecimal("300"));

        SaleOutCreateRequest request = new SaleOutCreateRequest();
        request.setWarehouseId(1L);
        request.setCustomerId(1L);
        request.setBizDate(LocalDate.now());
        request.setItems(List.of(item));

        mockMvc.perform(post("/api/v1/sale-outs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(300));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=SaleOutControllerTest test`
Expected: FAIL because sale DTO/service do not exist and controller has no endpoint

- [ ] **Step 3: Write minimal implementation**

Implement sale create/list/detail/update/delete with:
- order number generation like `SOyyyyMMddHHmmssSSS`
- FIFO batch selection from `inv_batch` with positive `remain_qty`
- inventory decrement in `inv_stock`
- deduction rows in `inv_sale_batch_deduction`
- negative stock flow rows in `inv_stock_flow`
- throw `BusinessException(400, "insufficient-stock")` when stock is not enough

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=SaleOutControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/sale src/main/java/org/liangyou/modules/inventory/entity/InvSaleBatchDeduction.java src/main/java/org/liangyou/modules/inventory/mapper/InvSaleBatchDeductionMapper.java src/test/java/org/liangyou/modules/sale/controller/SaleOutControllerTest.java
git commit -m "feat: implement sale outbound api"
```

### Task 5: Implement inventory and statistics query APIs

**Files:**
- Create: `src/main/java/org/liangyou/modules/inventory/dto/InventoryQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/inventory/dto/InventoryUpdateRequest.java`
- Create: `src/main/java/org/liangyou/modules/inventory/dto/InventoryFlowQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/inventory/vo/InventoryResponse.java`
- Create: `src/main/java/org/liangyou/modules/inventory/vo/InventoryBatchResponse.java`
- Create: `src/main/java/org/liangyou/modules/inventory/vo/InventoryFlowResponse.java`
- Create: `src/main/java/org/liangyou/modules/inventory/service/InventoryService.java`
- Modify: `src/main/java/org/liangyou/modules/inventory/controller/InventoryController.java`
- Create: `src/main/java/org/liangyou/modules/statistics/dto/StatisticsQueryRequest.java`
- Create: `src/main/java/org/liangyou/modules/statistics/vo/DailyInboundOutboundResponse.java`
- Create: `src/main/java/org/liangyou/modules/statistics/vo/AmountSummaryResponse.java`
- Create: `src/main/java/org/liangyou/modules/statistics/entity/StatDailySummary.java`
- Create: `src/main/java/org/liangyou/modules/statistics/entity/StatMonthlySummary.java`
- Create: `src/main/java/org/liangyou/modules/statistics/mapper/StatDailySummaryMapper.java`
- Create: `src/main/java/org/liangyou/modules/statistics/mapper/StatMonthlySummaryMapper.java`
- Create: `src/main/java/org/liangyou/modules/statistics/service/StatisticsService.java`
- Modify: `src/main/java/org/liangyou/modules/statistics/controller/StatisticsController.java`
- Test: `src/test/java/org/liangyou/modules/inventory/controller/InventoryControllerTest.java`
- Test: `src/test/java/org/liangyou/modules/statistics/controller/StatisticsControllerTest.java`

- [ ] **Step 1: Write the failing test**

`InventoryControllerTest`

```java
package org.liangyou.modules.inventory.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import(SecurityConfig.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    @Test
    void listInventoryEndpointExists() throws Exception {
        mockMvc.perform(get("/api/v1/inventories"))
                .andExpect(status().isOk());
    }
}
```

`StatisticsControllerTest`

```java
package org.liangyou.modules.statistics.controller;

import org.junit.jupiter.api.Test;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.statistics.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatisticsController.class)
@Import(SecurityConfig.class)
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticsService statisticsService;

    @Test
    void dailyStatisticsEndpointExists() throws Exception {
        mockMvc.perform(get("/api/v1/statistics/daily/inbound-outbound"))
                .andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=InventoryControllerTest,StatisticsControllerTest test`
Expected: FAIL because service APIs and endpoints do not exist

- [ ] **Step 3: Write minimal implementation**

Implement inventory service endpoints:
- list inventory summary from `inv_stock`
- query batches from `inv_batch`
- query flows from `inv_stock_flow`
- update inventory summary and write adjustment flow
- logical delete inventory summary

Implement statistics service endpoints:
- daily/monthly in/out with summary-table-first and fallback aggregation
- amount-by-category
- amount-by-customer
- amount-by-supplier

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=InventoryControllerTest,StatisticsControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/liangyou/modules/inventory src/main/java/org/liangyou/modules/statistics src/test/java/org/liangyou/modules/inventory/controller/InventoryControllerTest.java src/test/java/org/liangyou/modules/statistics/controller/StatisticsControllerTest.java
git commit -m "feat: implement inventory and statistics apis"
```
