# Basic Data and Statistics Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the confirmed enterprise-grade basic-data backend and frontend, then upgrade business pages and statistics pages to use real master data, chart switching, and Excel export.

**Architecture:** Add a new backend `basic` module exposing `/api/v1/basic/**` REST APIs over the existing SQL tables, with logical delete, status toggles, reference protection, option endpoints, and `.xlsx` export. Then replace frontend basic-data placeholders with Vue 2 + Element UI CRUD pages, wire purchase/sale/inventory filters to master-data selectors, and redesign statistics pages around scenario-based analysis and chart switching.

**Tech Stack:** Java 17, Spring Boot 3.2, Spring MVC, Validation, MyBatis Plus, JUnit 5, MockMvc, Vue 2.6, Element UI 2, Axios, ECharts.

---

## File Structure

Backend create:

- `src/main/java/org/liangyou/modules/basic/entity/BaseWarehouse.java`
- `src/main/java/org/liangyou/modules/basic/entity/BaseProductCategory.java`
- `src/main/java/org/liangyou/modules/basic/entity/BaseUnit.java`
- `src/main/java/org/liangyou/modules/basic/entity/BaseProductUnitConvert.java`
- `src/main/java/org/liangyou/modules/basic/entity/BaseSupplier.java`
- `src/main/java/org/liangyou/modules/basic/entity/BaseCustomer.java`
- `src/main/java/org/liangyou/modules/basic/mapper/*.java`
- `src/main/java/org/liangyou/modules/basic/dto/*Request.java`
- `src/main/java/org/liangyou/modules/basic/vo/*Response.java`
- `src/main/java/org/liangyou/modules/basic/service/BasicDataService.java`
- `src/main/java/org/liangyou/modules/basic/service/ExcelExportService.java`
- `src/main/java/org/liangyou/modules/basic/controller/*Controller.java`
- `src/test/java/org/liangyou/modules/basic/controller/*ControllerTest.java`
- `src/test/java/org/liangyou/modules/basic/service/BasicDataServiceTest.java`

Backend modify:

- `src/main/java/org/liangyou/modules/product/entity/BaseProduct.java`
- `src/main/java/org/liangyou/config/OpenApiConfig.java`
- `src/main/java/org/liangyou/config/SecurityConfig.java`
- `src/main/java/org/liangyou/modules/purchase/service/PurchaseInService.java`
- `src/main/java/org/liangyou/modules/purchase/vo/PurchaseInDetailResponse.java`
- `src/main/java/org/liangyou/modules/sale/service/SaleOutService.java`
- `src/main/java/org/liangyou/modules/sale/vo/SaleOutDetailResponse.java`
- `src/main/java/org/liangyou/modules/inventory/service/InventoryService.java`
- `src/main/java/org/liangyou/modules/inventory/vo/*.java`
- `src/main/java/org/liangyou/modules/statistics/controller/StatisticsController.java`
- `src/main/java/org/liangyou/modules/statistics/service/StatisticsService.java`
- `sql/grain_oil_ims_init.sql`
- `sql/grain_oil_ims_patch_2026_10_11_basic_data_status_columns.sql`

Frontend create:

- `frontend/src/api/basic/product.js`
- `frontend/src/api/basic/category.js`
- `frontend/src/api/basic/unit.js`
- `frontend/src/api/basic/unitConvert.js`
- `frontend/src/api/basic/warehouse.js`
- `frontend/src/api/basic/supplier.js`
- `frontend/src/api/basic/customer.js`
- `frontend/src/utils/download.js`
- `frontend/src/components/BasicDataSelect/index.vue`
- `frontend/src/components/ChartSwitcher/index.vue`
- `frontend/src/views/basic/product/index.vue`
- `frontend/src/views/basic/category/index.vue`
- `frontend/src/views/basic/unit/index.vue`
- `frontend/src/views/basic/unit-convert/index.vue`
- `frontend/src/views/basic/warehouse/index.vue`
- `frontend/src/views/basic/supplier/index.vue`
- `frontend/src/views/basic/customer/index.vue`
- `frontend/src/views/statistics/dashboard.vue`
- `frontend/src/views/statistics/trend.vue`
- `frontend/src/views/statistics/sale-product.vue`
- `frontend/src/views/statistics/purchase-product.vue`
- `frontend/src/views/statistics/inventory-structure.vue`
- `frontend/src/views/statistics/customer-rank.vue`
- `frontend/src/views/statistics/supplier-rank.vue`
- `frontend/src/views/statistics/category-share.vue`

Frontend modify:

- `frontend/src/router/index.js`
- `frontend/src/api/statistics/statistics.js`
- `frontend/src/views/purchase/in/index.vue`
- `frontend/src/views/purchase/in/detail.vue`
- `frontend/src/views/sale/out/index.vue`
- `frontend/src/views/sale/out/detail.vue`
- `frontend/src/views/inventory/stock/index.vue`
- `frontend/src/views/inventory/batch/index.vue`
- `frontend/src/views/inventory/flow/index.vue`

## Task 1: Backend Basic Data Models and Tests

**Files:**
- Create backend basic entities, mappers, DTOs, VOs.
- Test: `src/test/java/org/liangyou/modules/basic/service/BasicDataServiceTest.java`

- [ ] **Step 1: Write failing service tests**

Create `BasicDataServiceTest` with Mockito tests for:

```java
@Test
void deleteProductRejectsReferencedProduct() {
    when(purchaseInItemMapper.selectCount(any())).thenReturn(1L);

    BusinessException error = assertThrows(BusinessException.class,
            () -> service.deleteProduct(10L));

    assertEquals(400, error.getCode());
    assertEquals("basic-data-in-use", error.getMessage());
}

@Test
void productOptionsOnlyReturnEnabledProducts() {
    BaseProduct product = new BaseProduct();
    product.setId(1L);
    product.setProductCode("P001");
    product.setProductName("东北大米");
    product.setStatus(1);
    when(baseProductMapper.selectList(any())).thenReturn(List.of(product));

    List<BasicOptionResponse> options = service.productOptions(null);

    assertEquals(1, options.size());
    assertEquals("P001", options.get(0).getCode());
}
```

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=BasicDataServiceTest test`

Expected: compilation fails because `BasicDataService`, `BasicOptionResponse`, and basic-data methods do not exist.

- [ ] **Step 3: Implement entities, DTOs, VOs, mappers, and service methods**

Implement:

- Table entities for warehouse, category, unit, unit conversion, supplier, customer.
- Generic option VO: `id`, `code`, `name`, `status`.
- Page/query/create/update/status request DTOs per resource.
- Basic CRUD service methods per resource.
- Reference checks before delete.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -Dtest=BasicDataServiceTest test`

Expected: tests pass.

## Task 2: Backend Basic Data Controllers

**Files:**
- Create: `src/main/java/org/liangyou/modules/basic/controller/*Controller.java`
- Test: `src/test/java/org/liangyou/modules/basic/controller/*ControllerTest.java`

- [ ] **Step 1: Write failing MockMvc tests**

Create controller tests covering:

```java
mockMvc.perform(get("/api/v1/basic/products")
        .param("pageNum", "1")
        .param("pageSize", "10"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(0));

mockMvc.perform(get("/api/v1/basic/products/options"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(0));

mockMvc.perform(patch("/api/v1/basic/products/1/status")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":0}"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.code").value(0));
```

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest='*Basic*ControllerTest' test`

Expected: endpoint tests fail with 404 or missing controller classes.

- [ ] **Step 3: Implement REST controllers**

For each resource implement:

- `GET /api/v1/basic/{resources}`
- `GET /api/v1/basic/{resources}/{id}`
- `POST /api/v1/basic/{resources}`
- `PUT /api/v1/basic/{resources}/{id}`
- `DELETE /api/v1/basic/{resources}/{id}`
- `PATCH /api/v1/basic/{resources}/{id}/status`
- `GET /api/v1/basic/{resources}/options`
- `GET /api/v1/basic/{resources}/export`

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -Dtest='*Basic*ControllerTest' test`

Expected: endpoint tests pass.

## Task 3: Excel Export Support

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/org/liangyou/modules/basic/service/ExcelExportService.java`
- Test: `src/test/java/org/liangyou/modules/basic/service/ExcelExportServiceTest.java`

- [ ] **Step 1: Write failing export tests**

Test that exported bytes start with the XLSX zip header:

```java
byte[] bytes = excelExportService.export("商品", List.of(Map.of("商品编码", "P001")));
assertEquals('P', bytes[0]);
assertEquals('K', bytes[1]);
```

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=ExcelExportServiceTest test`

Expected: fails because export service and Excel dependency do not exist.

- [ ] **Step 3: Add Apache POI and implement export**

Add `org.apache.poi:poi-ooxml`, generate a single-sheet workbook, stream bytes from controllers with the correct content type and `Content-Disposition`.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -Dtest=ExcelExportServiceTest test`

Expected: export tests pass.

## Task 4: Business Module Master Data Linkage

**Files:**
- Modify purchase, sale, inventory services and VOs.
- Test existing purchase/sale/inventory controller tests and add name fields.

- [ ] **Step 1: Write failing tests for validation and name echo**

Add tests that:

- Creating purchase with disabled supplier returns `basic-data-disabled`.
- Purchase detail includes `warehouseName`, `supplierName`, `productName`, `unitName`.
- Creating sale with disabled customer returns `basic-data-disabled`.
- Inventory list includes `warehouseName`, `productName`, `categoryName`, `unitName`.

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest='PurchaseInControllerTest,SaleOutControllerTest,InventoryControllerTest' test`

Expected: tests fail because validation and name fields are missing.

- [ ] **Step 3: Implement validation and name enrichment**

Before create/update:

- Validate warehouse, supplier/customer, product, and unit exist and are enabled.
- Convert selected unit to base unit when conversion exists.
- Enrich responses with display names.

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -Dtest='PurchaseInControllerTest,SaleOutControllerTest,InventoryControllerTest' test`

Expected: tests pass.

## Task 5: Statistics Backend Upgrade

**Files:**
- Modify: `StatisticsController`, `StatisticsService`, statistics VOs.
- Test: `StatisticsControllerTest`

- [ ] **Step 1: Write failing statistics tests**

Add tests for:

- `GET /api/v1/statistics/dashboard`
- `GET /api/v1/statistics/trends`
- `GET /api/v1/statistics/sale-products`
- `GET /api/v1/statistics/purchase-products`
- `GET /api/v1/statistics/inventory-structure`
- `GET /api/v1/statistics/customer-ranks`
- `GET /api/v1/statistics/supplier-ranks`
- `GET /api/v1/statistics/category-shares`
- matching `/export` endpoints

- [ ] **Step 2: Run tests and verify RED**

Run: `mvn -Dtest=StatisticsControllerTest test`

Expected: new endpoints fail with 404.

- [ ] **Step 3: Implement scenario-based statistics**

Implement aggregated responses with:

- metric cards
- rows with dimension id/code/name
- amount, quantity, amountRate, qtyRate, rankNo
- trend rows for date/month dimensions

- [ ] **Step 4: Run tests and verify GREEN**

Run: `mvn -Dtest=StatisticsControllerTest test`

Expected: statistics tests pass.

## Task 6: Frontend Basic Data Pages

**Files:**
- Create basic API wrappers and seven basic pages.
- Modify router.

- [ ] **Step 1: Write failing frontend tests where practical**

Add unit tests for:

- `downloadBlob` sets a generated filename.
- `BasicDataSelect` loads options and emits selected value.
- `ChartSwitcher` switches chart type.

Run: `cd frontend && npm test -- --runInBand`

Expected: fails because utilities/components do not exist.

- [ ] **Step 2: Implement basic data API wrappers and pages**

Implement seven pages with:

- query form
- table
- create/edit dialog or drawer
- detail display
- delete/status confirmations
- export button
- pagination

- [ ] **Step 3: Run frontend tests**

Run: `cd frontend && npm test -- --runInBand`

Expected: tests pass.

## Task 7: Frontend Business Linkage

**Files:**
- Modify purchase, sale, inventory pages.

- [ ] **Step 1: Write or update tests for selector components**

Assert purchase/sale pages use `BasicDataSelect` for warehouse, supplier/customer, product, and unit selectors.

- [ ] **Step 2: Replace numeric ID inputs**

Update forms and filters:

- warehouse selector
- supplier/customer selector
- product selector
- unit selector
- name display in tables and details

- [ ] **Step 3: Run frontend tests**

Run: `cd frontend && npm test -- --runInBand`

Expected: tests pass.

## Task 8: Frontend Statistics Redesign

**Files:**
- Create statistics scenario pages and chart switcher.
- Modify router and statistics API wrapper.

- [ ] **Step 1: Write failing chart switcher tests**

Assert switching among `pie`, `bar`, `line`, and `table` updates rendered state and does not mutate source rows.

- [ ] **Step 2: Implement statistics pages**

Implement:

- dashboard
- trend
- sale product
- purchase product
- inventory structure
- customer rank
- supplier rank
- category share

Each page uses common filters, metric cards, `ChartSwitcher`, table, and export.

- [ ] **Step 3: Run frontend tests**

Run: `cd frontend && npm test -- --runInBand`

Expected: tests pass.

## Task 9: Full Verification

**Files:** all modified files.

- [ ] **Step 1: Run backend targeted tests**

Run:

```bash
mvn -Dtest='*Basic*Test,PurchaseInControllerTest,SaleOutControllerTest,InventoryControllerTest,StatisticsControllerTest' test
```

Expected: all targeted backend tests pass.

- [ ] **Step 2: Run backend full tests**

Run:

```bash
mvn test
```

Expected: Maven exits 0.

- [ ] **Step 3: Run frontend tests**

Run:

```bash
cd frontend && npm test -- --runInBand
```

Expected: Jest exits 0.

- [ ] **Step 4: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: Vue build exits 0.

- [ ] **Step 5: Review git diff**

Run:

```bash
git status --short
git diff --stat
```

Expected: only planned backend, frontend, SQL, and docs files are modified.

