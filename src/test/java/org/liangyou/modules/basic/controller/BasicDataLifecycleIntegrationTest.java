package org.liangyou.modules.basic.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.MethodSource;
import org.liangyou.modules.basic.dto.*;
import org.liangyou.modules.basic.service.BasicDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Exercises HTTP binding, validation, real MyBatis persistence and Excel output together. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:basic-lifecycle;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.cloud.nacos.config.enabled=false", "spring.config.import=", "security.jwt.secret=integration-secret"
})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("integration")
class BasicDataLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired BasicDataService basic;
    @Autowired org.liangyou.modules.basic.service.BasicImportService importer;

    @Test
    void importSupportsEveryBasicResourceAndRollsBackOnExistingCode() throws Exception {
        var samples = java.util.Map.of(
                "categories", new String[]{"面粉", "粮食", "1", "启用", ""},
                "units", new String[]{"NEW", "新单位", "启用", ""},
                "warehouses", new String[]{"WNEW", "新仓库", "", "", "", "启用", ""},
                "suppliers", new String[]{"SNEW", "新供应商", "", "", "", "", "", "", "启用", ""},
                "customers", new String[]{"CNEW", "新客户", "", "", "", "", "", "", "启用", ""},
                "products", new String[]{"PNEW", "新商品", "粮食", "", "", "", "", "BAG", "", "启用", ""},
                "unit-converts", new String[]{"P100", "BOX", "BAG", "10", "启用", ""});
        for (var entry : samples.entrySet()) {
            MockMultipartFile file = importFile(entry.getKey(), entry.getValue());
            mvc.perform(multipart("/api/v1/basic/" + entry.getKey() + "/import").file(file))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.successRows").value(1));
        }

        byte[] first = importer.template("units");
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(first)); var out = new java.io.ByteArrayOutputStream()) {
            var sheet = book.getSheetAt(0);
            var firstRow = sheet.createRow(1);
            var secondRow = sheet.createRow(2);
            for (int i = 0; i < 4; i++) firstRow.createCell(i).setCellValue(new String[]{"ROLLBACK", "待回滚", "启用", ""}[i]);
            for (int i = 0; i < 4; i++) secondRow.createCell(i).setCellValue(new String[]{"BAG", "已存在", "启用", ""}[i]);
            book.write(out);
            mvc.perform(multipart("/api/v1/basic/units/import").file(new MockMultipartFile("file", "units.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray())))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data.errors[0].rowNumber").value(3));
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM base_unit WHERE unit_code='ROLLBACK'", Integer.class));
    }

    @Test
    void selectedExportRejectsMissingIds() throws Exception {
        byte[] file = mvc.perform(post("/api/v1/basic/units/export").contentType(APPLICATION_JSON).content("{\"ids\":[100]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
            assertEquals("BAG", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        }
        mvc.perform(post("/api/v1/basic/units/export").contentType(APPLICATION_JSON).content("{\"ids\":[100,999]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void templateEndpointDownloadsChineseXlsx() throws Exception {
        byte[] file = mvc.perform(get("/api/v1/basic/units/import/template"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals("单位编码", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void categoryImportResolvesParentFromSameFileRegardlessOfRowOrder() throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(importer.template("categories")));
             var out = new java.io.ByteArrayOutputStream()) {
            var sheet = book.getSheetAt(0);
            var child = sheet.createRow(1);
            var parent = sheet.createRow(2);
            String[] childValues = {"子分类", "新父分类", "2", "启用", ""};
            String[] parentValues = {"新父分类", "", "1", "启用", ""};
            for (int i = 0; i < childValues.length; i++) child.createCell(i).setCellValue(childValues[i]);
            for (int i = 0; i < parentValues.length; i++) parent.createCell(i).setCellValue(parentValues[i]);
            book.write(out);
            mvc.perform(multipart("/api/v1/basic/categories/import").file(new MockMultipartFile("file", "categories.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray())))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.data.successRows").value(2));
        }
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM base_product_category child JOIN base_product_category parent ON child.parent_id=parent.id WHERE child.category_name='子分类' AND parent.category_name='新父分类'", Integer.class));
    }

    private MockMultipartFile importFile(String resource, String[] values) throws Exception {
        try (XSSFWorkbook book = new XSSFWorkbook(new ByteArrayInputStream(importer.template(resource)));
             var out = new java.io.ByteArrayOutputStream()) {
            var row = book.getSheetAt(0).createRow(1);
            for (int i = 0; i < values.length; i++) row.createCell(i).setCellValue(values[i]);
            book.write(out);
            return new MockMultipartFile("file", resource + ".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @AfterEach
    void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        jdbc.execute("DROP ALL OBJECTS");
        String init = Files.readString(Path.of("sql/grain_oil_ims_init.sql"));
        String ddl = init.substring(init.indexOf("CREATE TABLE "), init.indexOf("INSERT INTO sys_role"));
        try (var connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
        jdbc.update("INSERT INTO base_unit(id,unit_code,unit_name) VALUES(100,'BAG','袋'),(200,'BOX','箱')");
        jdbc.update("INSERT INTO base_product_category(id,category_name) VALUES(100,'粮食')");
        jdbc.update("INSERT INTO base_product(id,product_code,product_name,category_id,base_unit_id) VALUES(100,'P100','大米',100,100)");
    }

    static Stream<Resource> resources() {
        return Stream.of(
                new Resource("products", "base_product", "新增商品", "productName", "商品名称",
                        "{\"productCode\":\"NEW\",\"productName\":\"新增商品\",\"categoryId\":100,\"baseUnitId\":100}"),
                new Resource("categories", "base_product_category", "新增分类", "categoryName", "分类名称",
                        "{\"categoryName\":\"新增分类\",\"parentId\":0,\"sortNo\":1}"),
                new Resource("units", "base_unit", "新增单位", "unitName", "单位名称",
                        "{\"unitCode\":\"NEW\",\"unitName\":\"新增单位\"}"),
                new Resource("warehouses", "sys_warehouse", "新增仓库", "warehouseName", "仓库名称",
                        "{\"warehouseCode\":\"NEW\",\"warehouseName\":\"新增仓库\"}"),
                new Resource("suppliers", "base_supplier", "新增供应商", "supplierName", "供应商名称",
                        "{\"supplierCode\":\"NEW\",\"supplierName\":\"新增供应商\"}"),
                new Resource("customers", "base_customer", "新增客户", "customerName", "客户名称",
                        "{\"customerCode\":\"NEW\",\"customerName\":\"新增客户\"}"),
                new Resource("unit-converts", "base_product_unit_convert", "大米", "productName", "商品名称",
                        "{\"productId\":100,\"fromUnitId\":200,\"toUnitId\":100,\"convertRate\":10}")
        );
    }

    @ParameterizedTest(name = "{0}: create, edit, status, options, export and logical delete")
    @MethodSource("resources")
    void resourceLifecyclePersistsAndExportsThroughHttp(Resource resource) throws Exception {
        ObjectNode body = (ObjectNode) json.readTree(resource.body());
        String path = "/api/v1/basic/" + resource.path();
        JsonNode created = response(mvc.perform(post(path).contentType(APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andReturn().getResponse().getContentAsString());
        long id = created.path("data").path("id").asLong();
        assertTrue(id > 0);

        mvc.perform(get(path + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data." + resource.nameField()).value(resource.keyword()));
        mvc.perform(get(path).param("keyword", resource.keyword())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));

        body.put("remark", "修改已保存");
        mvc.perform(put(path + "/" + id).contentType(APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.remark").value("修改已保存"));
        mvc.perform(get(path + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.remark").value("修改已保存"));

        assertOptionPresent(path, resource.keyword(), id, true);
        mvc.perform(patch(path + "/" + id + "/status").contentType(APPLICATION_JSON).content("{\"status\":0}"))
                .andExpect(status().isOk());
        assertOptionPresent(path, resource.keyword(), id, false);
        mvc.perform(get(path + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0));

        // Saving an unrelated edit must preserve a previously disabled resource.
        mvc.perform(put(path + "/" + id).contentType(APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(0));

        byte[] bytes = mvc.perform(get(path + "/export").param("keyword", resource.keyword())
                        .param("status", "0").param("pageNum", "99").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"" + resource.path() + ".xlsx\""))
                .andReturn().getResponse().getContentAsByteArray();
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum(), "Export must ignore list pagination");
            boolean foundName = false;
            boolean foundStatus = false;
            boolean foundRemark = false;
            for (var cell : sheet.getRow(0)) {
                var dataCell = sheet.getRow(1).getCell(cell.getColumnIndex());
                String value = dataCell == null ? "" : dataCell.getStringCellValue();
                if (resource.exportNameHeader().equals(cell.getStringCellValue())) {
                    assertEquals(resource.keyword(), value);
                    foundName = true;
                }
                if ("状态".equals(cell.getStringCellValue())) {
                    assertEquals("停用", value);
                    foundStatus = true;
                }
                if ("备注".equals(cell.getStringCellValue())) {
                    assertEquals("修改已保存", value);
                    foundRemark = true;
                }
            }
            assertTrue(foundName && foundStatus && foundRemark);
        }

        mvc.perform(patch(path + "/" + id + "/status").contentType(APPLICATION_JSON).content("{\"status\":1}"))
                .andExpect(status().isOk());
        assertOptionPresent(path, resource.keyword(), id, true);
        mvc.perform(delete(path + "/" + id)).andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT deleted FROM " + resource.table() + " WHERE id = ?", Integer.class, id));
        mvc.perform(get(path + "/" + id)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(404));
        assertOptionPresent(path, resource.keyword(), id, false);
        mvc.perform(get(path).param("keyword", resource.keyword())).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @ParameterizedTest(name = "{0}: validation prevents writes")
    @MethodSource("resources")
    void invalidCreateAndStatusDoNotChangeDatabase(Resource resource) throws Exception {
        String path = "/api/v1/basic/" + resource.path();
        int initialCount = jdbc.queryForObject("SELECT COUNT(*) FROM " + resource.table(), Integer.class);
        mvc.perform(post(path).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        ObjectNode body = (ObjectNode) json.readTree(resource.body());
        body.put("status", 2);
        mvc.perform(post(path).contentType(APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        assertEquals(initialCount, jdbc.queryForObject("SELECT COUNT(*) FROM " + resource.table(), Integer.class));

        body.remove("status");
        long id = response(mvc.perform(post(path).contentType(APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        for (String invalidStatus : new String[]{"{}", "{\"status\":2}", "{\"status\":-1}"}) {
            mvc.perform(patch(path + "/" + id + "/status").contentType(APPLICATION_JSON).content(invalidStatus))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
        }
        mvc.perform(get(path + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value(1));
    }

    private void assertOptionPresent(String path, String keyword, long id, boolean expected) throws Exception {
        JsonNode options = response(mvc.perform(get(path + "/options").param("keyword", keyword))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        boolean found = false;
        for (JsonNode option : options) if (option.path("id").asLong() == id) found = true;
        assertEquals(expected, found, path + " options");
    }

    @ParameterizedTest(name = "{0}: update permission cannot change status")
    @MethodSource("resources")
    void editingStatusRequiresTheIndependentStatusPermission(Resource resource) throws Exception {
        String path = "/api/v1/basic/" + resource.path();
        ObjectNode body = (ObjectNode) json.readTree(resource.body());
        long id = response(mvc.perform(post(path).contentType(APPLICATION_JSON).content(body.toString()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
        String permission = "basic:" + switch (resource.path()) {
            case "categories" -> "category";
            case "unit-converts" -> "unit-convert";
            default -> resource.path().substring(0, resource.path().length() - 1);
        };
        authenticate(permission + ":update");
        body.put("status", 0);
        body.put("remark", "不能写入的修改");
        assertThrows(AccessDeniedException.class, () -> update(resource, id, body));
        assertEquals(1, jdbc.queryForObject("SELECT status FROM " + resource.table() + " WHERE id=?", Integer.class, id));
        assertNull(jdbc.queryForObject("SELECT remark FROM " + resource.table() + " WHERE id=?", String.class, id));

        // Ordinary edits may omit status or repeat its persisted value.
        body.remove("status");
        body.put("remark", "允许编辑");
        update(resource, id, body);
        body.put("status", 1);
        update(resource, id, body);
        assertEquals("允许编辑", jdbc.queryForObject("SELECT remark FROM " + resource.table() + " WHERE id=?", String.class, id));

        body.put("status", 0);
        authenticate(permission + ":update", "basic:another-resource:status");
        assertThrows(AccessDeniedException.class, () -> update(resource, id, body));
        for (String allowed : new String[]{permission + ":status", "ROLE_SUPER_ADMIN", "*:*:*"}) {
            authenticate(permission + ":update", allowed);
            update(resource, id, body);
            assertEquals(0, jdbc.queryForObject("SELECT status FROM " + resource.table() + " WHERE id=?", Integer.class, id));
            body.put("status", 1);
            update(resource, id, body);
            body.put("status", 0);
        }
    }

    private void authenticate(String... authorities) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("editor", "unused",
                Arrays.stream(authorities).map(SimpleGrantedAuthority::new).toList()));
    }

    private void update(Resource resource, long id, JsonNode body) throws Exception {
        switch (resource.path()) {
            case "products" -> basic.updateProduct(id, json.treeToValue(body, ProductSaveRequest.class));
            case "categories" -> basic.updateCategory(id, json.treeToValue(body, CategorySaveRequest.class));
            case "units" -> basic.updateUnit(id, json.treeToValue(body, UnitSaveRequest.class));
            case "unit-converts" -> basic.updateUnitConvert(id, json.treeToValue(body, UnitConvertSaveRequest.class));
            case "warehouses" -> basic.updateWarehouse(id, json.treeToValue(body, WarehouseSaveRequest.class));
            case "suppliers" -> basic.updateSupplier(id, json.treeToValue(body, SupplierSaveRequest.class));
            case "customers" -> basic.updateCustomer(id, json.treeToValue(body, CustomerSaveRequest.class));
            default -> throw new IllegalArgumentException(resource.path());
        }
    }

    private JsonNode response(String content) throws Exception { return json.readTree(content); }

    record Resource(String path, String table, String keyword, String nameField, String exportNameHeader, String body) {
        @Override public String toString() { return path; }
    }
}
