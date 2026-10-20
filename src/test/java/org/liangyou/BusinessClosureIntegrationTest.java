package org.liangyou;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.liangyou.modules.statistics.dto.StatisticsQueryRequest;
import org.liangyou.modules.statistics.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.dto.ProductQueryRequest;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.liangyou.modules.purchase.service.PurchaseInService;
import org.liangyou.modules.purchase.dto.PurchaseInCreateRequest;
import org.liangyou.modules.purchase.dto.PurchaseInItemRequest;
import org.liangyou.modules.sale.service.SaleOutService;
import org.liangyou.modules.sale.service.SaleDocumentExportService;
import org.liangyou.modules.sale.dto.SaleOutCreateRequest;
import org.liangyou.modules.sale.dto.SaleOutItemRequest;
import org.liangyou.modules.inventory.service.InventoryService;
import org.liangyou.modules.inventory.dto.InventoryUpdateRequest;
import org.liangyou.common.exception.BusinessException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Runs actual MyBatis SQL against an isolated database; never touches the developer database. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:closure;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.cloud.nacos.config.enabled=false", "spring.config.import=", "security.jwt.secret=integration-secret"
})
@ActiveProfiles("integration")
class BusinessClosureIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired StatisticsService statistics;
    @Autowired PurchaseInService purchases;
    @Autowired SaleOutService sales;
    @Autowired SaleDocumentExportService saleExports;
    @Autowired InventoryService inventory;
    @Autowired BasicDataService basic;
    @Autowired ExcelExportService excel;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void schemaAndMasterData() throws Exception {
        jdbc.execute("DROP ALL OBJECTS");
        String init = Files.readString(Path.of("sql/grain_oil_ims_init.sql"));
        String ddl = init.substring(init.indexOf("CREATE TABLE "), init.indexOf("INSERT INTO sys_role"));
        try (var connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
        jdbc.update("INSERT INTO sys_warehouse(id,warehouse_code,warehouse_name) VALUES(1,'W1','总仓'),(2,'W2','分仓')");
        jdbc.update("INSERT INTO base_unit(id,unit_code,unit_name) VALUES(1,'BAG','袋'),(2,'BOX','箱')");
        jdbc.update("INSERT INTO base_product_category(id,category_name) VALUES(1,'粮食')");
        jdbc.update("INSERT INTO base_product(id,product_code,product_name,category_id,base_unit_id) VALUES(1,'P1','大米',1,1),(2,'P2','面粉',1,1)");
        jdbc.update("INSERT INTO base_supplier(id,supplier_code,supplier_name) VALUES(1,'S1','粮食供应商')");
        jdbc.update("INSERT INTO base_customer(id,customer_code,customer_name) VALUES(1,'C1','门店客户')");
    }

    @Test
    void confirmedPickupReservesStockAndCompletesExactlyOnce() {
        purchases.create(purchase("10", 1, "10"));
        SaleOutCreateRequest request = sale("3", 1);
        long id = sales.create(request);
        assertStock("10", "100");
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM inv_sale_batch_deduction WHERE sale_out_id=?", Integer.class, id));
        sales.confirm(id);
        assertEquals(0, jdbc.queryForObject("SELECT total_qty FROM inv_stock WHERE product_id=1", BigDecimal.class).compareTo(new BigDecimal("10")));
        assertEquals(0, jdbc.queryForObject("SELECT locked_qty FROM inv_stock WHERE product_id=1", BigDecimal.class).compareTo(new BigDecimal("3")));
        sales.handoff(id);
        assertStock("7", "70");
        assertEquals(2, sales.detail(id).getEvents().size());
        assertEquals(0, jdbc.queryForObject("SELECT locked_qty FROM inv_stock WHERE product_id=1", BigDecimal.class).compareTo(BigDecimal.ZERO));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM inv_sale_batch_deduction WHERE sale_out_id=?", Integer.class, id));
        assertThrows(BusinessException.class, () -> sales.handoff(id));
    }

    @Test
    void failedDeliveryKeepsReservationUntilGoodsReturn() {
        purchases.create(purchase("10", 1, "10"));
        SaleOutCreateRequest request = sale("4", 1);
        request.setFulfillmentType("delivery");
        request.setContactName("张三"); request.setContactPhone("13800000000"); request.setDeliveryAddress("A路1号");
        long id = sales.create(request);
        sales.confirm(id);
        sales.startDelivery(id);
        var stock = inventory.query(new org.liangyou.modules.inventory.dto.InventoryQueryRequest()).getList().get(0);
        decimal("4", stock.getInDeliveryQty());
        sales.deliveryFailed(id, "客户拒收");
        decimal("4", jdbc.queryForObject("SELECT locked_qty FROM inv_stock WHERE product_id=1", BigDecimal.class));
        sales.returned(id);
        decimal("0", inventory.query(new org.liangyou.modules.inventory.dto.InventoryQueryRequest()).getList().get(0).getInDeliveryQty());
        decimal("0", jdbc.queryForObject("SELECT locked_qty FROM inv_stock WHERE product_id=1", BigDecimal.class));
        decimal("10", jdbc.queryForObject("SELECT total_qty FROM inv_stock WHERE product_id=1", BigDecimal.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM inv_sale_batch_deduction WHERE sale_out_id=?", Integer.class, id));
    }

    @Test
    void guestOrderUsesSnapshotAndOnlyCompletionEntersSalesReport() {
        purchases.create(purchase("10", 1, "10"));
        SaleOutCreateRequest request = sale("2", 1);
        request.setCustomerId(null); request.setCustomerName("散客");
        long id = sales.create(request);
        assertEquals("散客", sales.detail(id).getCustomerName());
        sales.confirm(id);
        assertTrue(statistics.saleProducts(period()).getRows().isEmpty());
        sales.handoff(id);
        assertEquals(1, statistics.saleProducts(period()).getRows().size());
        assertEquals("散客", statistics.customerRanks(period()).getRows().get(0).getDimensionName());
    }

    @Test
    void guestRankingCombinesDifferentOrderNicknames() {
        purchases.create(purchase("10", 1, "10"));
        var first = sale("2", 1); first.setCustomerId(null); first.setCustomerName("张先生");
        var second = sale("3", 1); second.setCustomerId(null); second.setCustomerName("李女士");
        long firstId = sales.create(first); long secondId = sales.create(second);
        sales.confirm(firstId); sales.handoff(firstId);
        sales.confirm(secondId); sales.handoff(secondId);
        assertNull(jdbc.queryForObject("SELECT customer_id FROM biz_sale_out WHERE id=?", Long.class, secondId));
        var ranking = statistics.customerRanks(period());
        assertEquals(1, ranking.getRows().size());
        assertEquals("散客", ranking.getRows().get(0).getDimensionName());
        decimal("5", ranking.getRows().get(0).getQty());
    }

    @Test
    void editingDraftToGuestClearsCustomerAndDeliveryFields() {
        purchases.create(purchase("10", 1, "10"));
        var request = sale("2", 1);
        request.setFulfillmentType("delivery");
        request.setContactName("张三"); request.setContactPhone("13800000000");
        request.setDeliveryAddress("A路1号"); request.setDeliveryRemark("后门卸货");
        request.setPlannedDeliveryTime(LocalDateTime.now().plusDays(1));
        long id = sales.create(request);
        var guest = sale("2", 1);
        guest.setCustomerId(null); guest.setCustomerName("散客");
        sales.update(id, guest);
        var detail = sales.detail(id);
        assertNull(detail.getCustomerId());
        assertNull(detail.getDeliveryRemark());
        assertNull(detail.getPlannedDeliveryTime());
        assertNull(detail.getContactName());
        assertNull(detail.getDeliveryAddress());
    }

    @Test
    void deliveryRemarkSurvivesSaveAndAppearsOnPrintedDocument() throws Exception {
        purchases.create(purchase("10", 1, "10"));
        var request = sale("2", 1);
        request.setFulfillmentType("delivery");
        request.setContactName("张三"); request.setContactPhone("13800000000");
        request.setDeliveryAddress("A路1号"); request.setDeliveryRemark("后门卸货");
        long id = sales.create(request);
        assertEquals("后门卸货", sales.detail(id).getDeliveryRemark());
        sales.confirm(id);
        try (PDDocument document = PDDocument.load(saleExports.deliveryPdf(id))) {
            assertTrue(new PDFTextStripper().getText(document).contains("后门卸货"));
        }
        var events = sales.detail(id).getEvents();
        assertEquals("print_pdf", events.get(events.size() - 1).getEventType());
        assertEquals("template=v1", events.get(events.size() - 1).getDetail());
    }

    @Test
    void dashboardSeparatesPendingSalesFromCompletedSales() {
        purchases.create(purchase("10", 1, "10"));
        long id = sales.create(sale("2", 1)); sales.confirm(id);
        var pending = statistics.dashboard(period()).getMetrics().stream()
                .filter(metric -> "pendingSaleAmount".equals(metric.getKey())).findFirst().orElseThrow();
        decimal("30", pending.getValue());
        sales.handoff(id);
        var completed = statistics.dashboard(period()).getMetrics().stream()
                .filter(metric -> "pendingSaleAmount".equals(metric.getKey())).findFirst().orElseThrow();
        decimal("0", completed.getValue());
    }

    @Test
    void quickCustomerGetsServerCodeAndCanBeFoundByPhone() {
        var customer = basic.quickCreateCustomer("李四", "李四", "13900000000", "B路2号");
        assertTrue(customer.getCustomerCode().startsWith("C"));
        assertEquals(customer.getId(), basic.searchCustomers("13900000000").get(0).getId());
    }

    @Test
    void confirmedSalesListExportsPersistedSnapshotAndComputedAmount() throws Exception {
        purchases.create(purchase("10", 1, "10"));
        SaleOutCreateRequest request = sale("2", 1);
        request.setCustomerName("本单客户");
        long id = sales.create(request);
        assertThrows(BusinessException.class, () -> saleExports.excel(id));
        sales.confirm(id);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(saleExports.excel(id)))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("本单客户", sheet.getRow(1).getCell(1).getStringCellValue());
            assertEquals("大米", sheet.getRow(4).getCell(0).getStringCellValue());
        }
    }

    @Test
    void pdfExportEmbedsReadableChineseAndRejectsDraft() throws Exception {
        purchases.create(purchase("10", 1, "10"));
        long id = sales.create(sale("2", 1));
        assertThrows(BusinessException.class, () -> saleExports.pdf(id));
        sales.confirm(id);
        try (PDDocument document = PDDocument.load(saleExports.pdf(id))) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("粮油配送中心销售清单"));
            assertTrue(text.contains("大米"));
        }
    }

    @Test
    void salesAnalysisAggregatesActualLinesAndKeepsDisabledNames() {
        jdbc.update("INSERT INTO biz_sale_out(id,order_no,warehouse_id,customer_id,biz_date,total_amount,order_status,completed_time,customer_name_snapshot) VALUES(1,'SO1',1,1,'2026-10-15',400,'completed','2026-10-15 12:00:00','门店客户')");
        jdbc.update("INSERT INTO biz_sale_out_item(sale_out_id,warehouse_id,product_id,unit_id,qty,base_qty,unit_price,amount) VALUES(1,1,1,1,3,3,100,300),(1,1,2,1,2,2,50,100)");
        jdbc.update("UPDATE base_product SET status=0 WHERE id=1");
        StatisticsQueryRequest request = new StatisticsQueryRequest();
        request.setDateFrom(LocalDate.of(2026,10,1));
        request.setDateTo(LocalDate.of(2026,10,31));
        var response = statistics.saleProducts(request);
        assertEquals(2, response.getRows().size());
        assertEquals("大米", response.getRows().get(0).getDimensionName());
        assertEquals(0, response.getRows().get(0).getAmount().compareTo(new BigDecimal("300")));
        assertEquals(0, response.getRows().get(0).getAmountRate().compareTo(new BigDecimal("75")));
        assertEquals(1, response.getRows().get(0).getRankNo());
        request.setWarehouseId(2L);
        assertTrue(statistics.saleProducts(request).getRows().isEmpty());
    }

    @Test
    void convertedPurchaseSaleUpdatesAndReversalsKeepSnapshotsAndStableIds() {
        conversion("10");
        long purchaseId = purchases.create(purchase("2", 2, "100"));
        assertStock("20", "200");
        long saleId = sales.create(sale("2", 1));
        String saleNo = sales.detail(saleId).getOrderNo();
        assertStock("20", "200");
        sales.confirm(saleId);
        decimal("2", jdbc.queryForObject("SELECT locked_qty FROM inv_stock", BigDecimal.class));
        sales.update(saleId, sale("3", 1));
        assertEquals(saleNo, sales.detail(saleId).getOrderNo());
        decimal("3", jdbc.queryForObject("SELECT locked_qty FROM inv_stock", BigDecimal.class));
        assertThrows(BusinessException.class, () -> sales.update(saleId, sale("100", 1)));
        decimal("3", jdbc.queryForObject("SELECT locked_qty FROM inv_stock", BigDecimal.class));
        decimal("3", sales.detail(saleId).getItems().get(0).getBaseQty());
        assertThrows(BusinessException.class, () -> purchases.delete(purchaseId));
        jdbc.update("UPDATE base_product_unit_convert SET convert_rate=20");
        jdbc.update("UPDATE base_product SET status=0 WHERE id=1");
        assertEquals("大米", sales.detail(saleId).getItems().get(0).getProductName());
        sales.handoff(saleId);
        assertStock("17", "170");
        assertThrows(BusinessException.class, () -> sales.delete(saleId));
        assertThrows(BusinessException.class, () -> purchases.delete(purchaseId));
    }

    @Test
    void purchaseUpdatePreservesUniqueOrderNumberAndRollbackIsAtomic() {
        conversion("10");
        long id = purchases.create(purchase("2", 2, "100"));
        String no = purchases.detail(id).getOrderNo();
        purchases.update(id, purchase("3", 2, "100"));
        assertEquals(no, purchases.detail(id).getOrderNo());
        assertStock("30", "300");
        jdbc.update("UPDATE base_product_unit_convert SET convert_rate=0.333333");
        // Not representable at inventory precision: all reversal mutations must roll back.
        assertThrows(BusinessException.class, () -> purchases.update(id, purchase("1", 2, "100")));
        assertStock("30", "300");
        decimal("30", purchases.detail(id).getItems().get(0).getBaseQty());
    }

    @Test
    void fifoCrossesBatchesAndReportsRealPartnerAndBaseQuantities() {
        conversion("10");
        purchases.create(purchase("2", 2, "100"));
        purchases.create(purchase("5", 1, "20"));
        long id = sales.create(sale("22", 1));
        sales.confirm(id); sales.handoff(id);
        assertStock("3", "60");
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM inv_sale_batch_deduction WHERE sale_out_id=?", Integer.class, id));
        StatisticsQueryRequest query = period();
        var customer = statistics.customerRanks(query);
        assertEquals("门店客户", customer.getRows().get(0).getDimensionName());
        decimal("22", customer.getTotalQty());
        decimal("330", customer.getTotalAmount());
        var supplier = statistics.supplierRanks(query);
        assertEquals("粮食供应商", supplier.getRows().get(0).getDimensionName());
        decimal("25", supplier.getTotalQty());
        decimal("300", supplier.getTotalAmount());
        decimal("60", statistics.inventoryStructure(query).getTotalAmount());
        query.setCustomerId(999L);
        assertTrue(statistics.saleProducts(query).getRows().isEmpty());
    }

    @Test
    void adjustmentsSynchronizeBatchesAndRejectInconsistentDeletion() {
        purchases.create(purchase("10", 1, "12"));
        long id = jdbc.queryForObject("SELECT id FROM inv_stock", Long.class);
        inventory.update(id, adjustment("12"));
        assertStock("12", "144");
        inventory.update(id, adjustment("7"));
        assertStock("7", "84");
        InventoryUpdateRequest invalid = adjustment("7"); invalid.setAvailableQty(new BigDecimal("6"));
        assertThrows(BusinessException.class, () -> inventory.update(id, invalid));
        assertThrows(BusinessException.class, () -> inventory.delete(id));
        decimal("7", jdbc.queryForObject("SELECT SUM(change_qty) FROM inv_stock_flow", BigDecimal.class));
    }

    @Test
    void concurrentSalesCannotOversellOrLoseBatchMovements() throws Exception {
        purchases.create(purchase("10", 1, "10"));
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> create = () -> {
                start.await();
                try { long id = sales.create(sale("8", 1)); sales.confirm(id); return true; }
                catch (BusinessException ex) { assertEquals("insufficient-stock", ex.getMessage()); return false; }
            };
            Future<Boolean> one = pool.submit(create); Future<Boolean> two = pool.submit(create); start.countDown();
            assertNotEquals(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS));
            assertStock("10", "100");
            decimal("8", jdbc.queryForObject("SELECT locked_qty FROM inv_stock", BigDecimal.class));
            assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM biz_sale_out WHERE deleted=0", Integer.class));
        } finally { pool.shutdownNow(); }
    }

    @Test
    void filteredExportsContainActualRowsBeyondPagination() throws Exception {
        ProductQueryRequest q = new ProductQueryRequest(); q.setPageSize(1);
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.exportProducts(q)))) {
            assertEquals(3, workbook.getSheetAt(0).getPhysicalNumberOfRows());
        }
        q.setKeyword("P1");
        assertEquals(1, basic.productOptions("P1").size());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.exportProducts(q)))) {
            assertEquals(2, workbook.getSheetAt(0).getPhysicalNumberOfRows());
        }
        purchases.create(purchase("10", 1, "10")); long exportedSale = sales.create(sale("3", 1)); sales.confirm(exportedSale); sales.handoff(exportedSale);
        for (String scene : List.of("dashboard", "trends", "sale-products", "purchase-products", "inventory-structure",
                "customer-ranks", "supplier-ranks", "category-shares")) {
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(statistics.exportScenario(scene, period())))) {
                assertTrue(workbook.getSheetAt(0).getPhysicalNumberOfRows() > 1, scene);
            }
        }
    }

    @Test
    void trendsFillMissingDaysAndCategoryFiltersIncludeDescendants() {
        jdbc.update("INSERT INTO base_product_category(id,category_name,parent_id) VALUES(2,'子分类',1)");
        jdbc.update("UPDATE base_product SET category_id=2 WHERE id=1");
        purchases.create(purchase("4", 1, "10"));
        long trendSale = sales.create(sale("2", 1)); sales.confirm(trendSale); sales.handoff(trendSale);
        StatisticsQueryRequest q = period(); q.setDateFrom(LocalDate.of(2026,10,15)); q.setDateTo(LocalDate.of(2026,10,20));
        q.setCategoryId(1L);
        var trends = statistics.trends(q);
        assertEquals(6, trends.getRows().size());
        decimal("40", trends.getRows().get(0).getPurchaseAmount());
        decimal("30", trends.getRows().get(5).getSaleAmount());
        decimal("0", trends.getRows().get(0).getSaleAmount());
        q.setGroupBy("month");
        assertEquals("2026-10", statistics.trends(q).getRows().get(0).getDimensionName());
        q.setStatType("inventory");
        decimal("20", statistics.categoryShares(q).getTotalAmount());
    }

    @Test
    void contactKeywordFiltersListsAndExports() throws Exception {
        jdbc.update("UPDATE base_supplier SET contact_name='赵经理',contact_phone='13800001234' WHERE id=1");
        jdbc.update("UPDATE base_customer SET contact_name='钱经理',contact_phone='13900005678' WHERE id=1");
        BasicQueryRequest q = new BasicQueryRequest(); q.setKeyword("01234");
        assertEquals(1, basic.querySuppliers(q).getTotal());
        q.setKeyword("钱经理"); assertEquals(1, basic.queryCustomers(q).getTotal());
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(excel.exportBasic("customers", q)))) {
            assertEquals(2, workbook.getSheetAt(0).getPhysicalNumberOfRows());
        }
    }

    @Test
    void purchaseAnalysisIdentifiesAllSupplierSourcesAndSupportsSupplierFilter() {
        jdbc.update("INSERT INTO base_supplier(id,supplier_code,supplier_name) VALUES(2,'S2','第二供应商')");
        purchases.create(purchase("2", 1, "10"));
        var second = purchase("3", 1, "20"); second.setSupplierId(2L); purchases.create(second);
        var q = period();
        var row = statistics.purchaseProducts(q).getRows().get(0);
        String suppliers = (String) new org.springframework.beans.BeanWrapperImpl(row).getPropertyValue("supplierName");
        assertTrue(suppliers.contains("粮食供应商")); assertTrue(suppliers.contains("第二供应商"));
        q.setSupplierId(2L);
        var filtered = statistics.purchaseProducts(q);
        decimal("60", filtered.getTotalAmount());
        assertEquals("第二供应商", new org.springframework.beans.BeanWrapperImpl(filtered.getRows().get(0)).getPropertyValue("supplierName"));
    }

    @Test
    void compatibilityStatisticsAggregateLiveDocumentsInBaseUnits() {
        conversion("10");
        long first = purchases.create(purchase("2", 2, "100"));
        long second = purchases.create(purchase("1", 2, "100"));
        long sale = sales.create(sale("1", 2)); sales.confirm(sale); sales.handoff(sale);
        var q = period(); q.setStatType("purchase");
        var daily = statistics.dailyInboundOutbound(q);
        assertEquals(1, daily.size());
        assertEquals("purchase", daily.get(0).getStatType());
        decimal("30", daily.get(0).getTotalQty());
        decimal("300", daily.get(0).getTotalAmount());
        q.setMonthFrom("2026-10"); q.setMonthTo("2026-10");
        var monthly = statistics.monthlyInboundOutbound(q);
        assertEquals(1, monthly.size());
        assertEquals("2026-10", monthly.get(0).getKey());
        decimal("30", monthly.get(0).getTotalQty());
        decimal("300", monthly.get(0).getTotalAmount());
        var supplier = statistics.amountBySupplier(q);
        assertEquals("supplier:1", supplier.get(0).getKey());
        decimal("30", supplier.get(0).getTotalQty());
        q.setStatType("sale");
        var customer = statistics.amountByCustomer(q);
        assertEquals("customer:1", customer.get(0).getKey());
        decimal("10", customer.get(0).getTotalQty());
        var category = statistics.amountByCategory(q);
        decimal("10", category.get(0).getTotalQty());
        q.setProductId(2L);
        assertTrue(statistics.dailyInboundOutbound(q).isEmpty());
        assertTrue(statistics.monthlyInboundOutbound(q).isEmpty());
        assertTrue(statistics.amountByCustomer(q).isEmpty());
        assertTrue(statistics.amountBySupplier(q).isEmpty());
        assertTrue(statistics.amountByCategory(q).isEmpty());
        assertThrows(BusinessException.class, () -> sales.delete(sale));
        assertThrows(BusinessException.class, () -> purchases.delete(first));
        purchases.delete(second);
        q.setProductId(null);
        decimal("10", statistics.amountByCustomer(q).get(0).getTotalQty());
    }

    @Test
    void inventoryCategoryFiltersAndExportsIncludeDescendants() throws Exception {
        jdbc.update("INSERT INTO base_product_category(id,category_name,parent_id) VALUES(2,'米类',1),(3,'精米',2)");
        jdbc.update("UPDATE base_product SET category_id=3 WHERE id=1");
        purchases.create(purchase("4", 1, "10"));
        var other = purchase("5", 1, "20"); other.getItems().get(0).setProductId(2L); purchases.create(other);
        jdbc.update("UPDATE base_product_category SET status=0 WHERE id=2");
        var q = new org.liangyou.modules.inventory.dto.InventoryQueryRequest(); q.setCategoryId(2L); q.setPageSize(1);
        var stocks = inventory.query(q);
        assertEquals(1, stocks.getTotal());
        assertEquals("大米", stocks.getList().get(0).getProductName());
        assertEquals(1, inventory.batches(null, null, 2L).size());
        var flows = new org.liangyou.modules.inventory.dto.InventoryFlowQueryRequest(); flows.setCategoryId(2L);
        assertEquals(1, inventory.flows(flows).size());
        for (byte[] bytes : List.of(inventory.exportStocks(q), inventory.exportBatches(q), inventory.exportFlows(flows))) {
            try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                assertEquals(2, workbook.getSheetAt(0).getPhysicalNumberOfRows());
            }
        }
    }

    private void conversion(String rate) {
        jdbc.update("INSERT INTO base_product_unit_convert(product_id,from_unit_id,to_unit_id,convert_rate) VALUES(1,2,1,?)", new BigDecimal(rate));
    }
    private PurchaseInCreateRequest purchase(String qty, long unit, String price) {
        PurchaseInItemRequest item = new PurchaseInItemRequest(); item.setProductId(1L); item.setUnitId(unit);
        item.setQty(new BigDecimal(qty)); item.setUnitPrice(new BigDecimal(price)); item.setAmount(BigDecimal.ZERO);
        item.setBatchNo("B"+java.util.UUID.randomUUID()); item.setInboundTime(LocalDateTime.of(2026,10,15,9,0));
        PurchaseInCreateRequest q = new PurchaseInCreateRequest(); q.setWarehouseId(1L); q.setSupplierId(1L);
        q.setBizDate(LocalDate.of(2026,10,15)); q.setItems(List.of(item)); return q;
    }
    private SaleOutCreateRequest sale(String qty, long unit) {
        SaleOutItemRequest item = new SaleOutItemRequest(); item.setProductId(1L); item.setUnitId(unit);
        item.setQty(new BigDecimal(qty)); item.setUnitPrice(new BigDecimal("15")); item.setAmount(BigDecimal.ZERO);
        SaleOutCreateRequest q = new SaleOutCreateRequest(); q.setWarehouseId(1L); q.setCustomerId(1L);
        q.setBizDate(LocalDate.of(2026,10,15)); q.setItems(List.of(item)); return q;
    }
    private InventoryUpdateRequest adjustment(String qty) {
        InventoryUpdateRequest q = new InventoryUpdateRequest(); q.setTotalQty(new BigDecimal(qty));
        q.setAvailableQty(new BigDecimal(qty)); q.setRemark("盘点核实"); return q;
    }
    private StatisticsQueryRequest period() {
        StatisticsQueryRequest q = new StatisticsQueryRequest();
        q.setDateFrom(LocalDate.of(2026,10,1)); q.setDateTo(LocalDate.of(2026,10,31)); return q;
    }
    private void assertStock(String qty, String value) {
        decimal(qty, jdbc.queryForObject("SELECT total_qty FROM inv_stock WHERE warehouse_id=1 AND product_id=1", BigDecimal.class));
        decimal(qty, jdbc.queryForObject("SELECT COALESCE(SUM(remain_qty),0) FROM inv_batch WHERE deleted=0", BigDecimal.class));
        decimal(value, jdbc.queryForObject("SELECT COALESCE(SUM(remain_qty*unit_price),0) FROM inv_batch WHERE deleted=0", BigDecimal.class));
    }
    private void decimal(String expected, BigDecimal actual) { assertEquals(0, new BigDecimal(expected).compareTo(actual), expected+" vs "+actual); }
}
