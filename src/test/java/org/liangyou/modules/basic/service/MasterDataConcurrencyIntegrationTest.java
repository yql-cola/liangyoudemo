package org.liangyou.modules.basic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.basic.dto.BasicQueryRequest;
import org.liangyou.modules.basic.dto.ProductSaveRequest;
import org.liangyou.modules.basic.dto.CategorySaveRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:master-locks;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.cloud.nacos.config.enabled=false", "spring.config.import=", "security.jwt.secret=integration-secret"
})
@ActiveProfiles("integration")
class MasterDataConcurrencyIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired BasicDataService basic;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void createIsolatedSchema() throws Exception {
        jdbc.execute("DROP ALL OBJECTS");
        String init = Files.readString(Path.of("sql/grain_oil_ims_init.sql"));
        String ddl = init.substring(init.indexOf("CREATE TABLE "), init.indexOf("INSERT INTO sys_role"));
        try (var connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
        jdbc.update("INSERT INTO sys_warehouse(id,warehouse_code,warehouse_name) VALUES(1,'W1','总仓')");
        jdbc.update("INSERT INTO base_unit(id,unit_code,unit_name) VALUES(1,'BAG','袋')");
        jdbc.update("INSERT INTO base_product_category(id,category_name) VALUES(1,'粮食')");
        jdbc.update("INSERT INTO base_product(id,product_code,product_name,category_id,base_unit_id) VALUES(1,'P1','大米',1,1)");
        jdbc.update("INSERT INTO base_supplier(id,supplier_code,supplier_name,contact_name,contact_phone) VALUES(1,'S1','供应商','采购联系人','13800138000')");
        jdbc.update("INSERT INTO base_customer(id,customer_code,customer_name,contact_name,contact_phone) VALUES(1,'C1','客户','销售联系人','13900139000')");
    }

    @Test
    void contactKeywordsSelectTheSameRecordsForListsAndExports() {
        BasicQueryRequest q = new BasicQueryRequest();
        for (String keyword : List.of("采购联系", "13800138")) {
            q.setKeyword(keyword);
            assertEquals(1, basic.querySuppliers(q).getTotal());
            assertEquals(1, basic.exportBasic("suppliers", q).size());
        }
        for (String keyword : List.of("销售联系", "13900139")) {
            q.setKeyword(keyword);
            assertEquals(1, basic.queryCustomers(q).getTotal());
            assertEquals(1, basic.exportBasic("customers", q).size());
        }
        q.setKeyword("不存在");
        assertEquals(0, basic.queryCustomers(q).getTotal());
        assertTrue(basic.exportBasic("suppliers", q).isEmpty());
    }

    @Test
    void supplierDeletionWaitsForValidatedBusinessAndThenSeesItsReference() throws Exception {
        CountDownLatch validated = new CountDownLatch(1);
        CountDownLatch commitBusiness = new CountDownLatch(1);
        CountDownLatch deletionStarted = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> business = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                basic.validatePurchaseMasterData(1L, 1L, List.of(1L), List.of(1L));
                validated.countDown();
                await(commitBusiness);
                jdbc.update("INSERT INTO biz_purchase_in(order_no,warehouse_id,supplier_id,biz_date) VALUES('PI1',1,1,'2026-10-18')");
            }));
            assertTrue(validated.await(5, TimeUnit.SECONDS));
            Future<?> deletion = workers.submit(() -> {
                deletionStarted.countDown();
                basic.deleteSupplier(1L);
            });
            assertTrue(deletionStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> deletion.get(300, TimeUnit.MILLISECONDS));
            commitBusiness.countDown();
            business.get(5, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class, () -> deletion.get(5, TimeUnit.SECONDS));
            assertInstanceOf(BusinessException.class, failure.getCause());
            assertEquals("basic-data-in-use", failure.getCause().getMessage());
            assertEquals("供应商", basic.supplierDetail(1L).getSupplierName());
        } finally {
            commitBusiness.countDown();
            workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void changingBaseUnitWaitsForValidatedBusinessAndThenRejectsItsReference() throws Exception {
        jdbc.update("INSERT INTO base_unit(id,unit_code,unit_name) VALUES(2,'BOX','箱')");
        ProductSaveRequest edit = new ProductSaveRequest();
        edit.setProductCode("P1"); edit.setProductName("大米"); edit.setCategoryId(1L); edit.setBaseUnitId(2L);
        assertMutationWaitsAndRejects(
                () -> basic.validatePurchaseMasterData(1L, 1L, List.of(1L), List.of(1L)),
                () -> {
                    jdbc.update("INSERT INTO biz_purchase_in(id,order_no,warehouse_id,supplier_id,biz_date) VALUES(1,'PI1',1,1,'2026-10-18')");
                    jdbc.update("INSERT INTO biz_purchase_in_item(purchase_in_id,warehouse_id,product_id,unit_id,qty,base_qty,unit_price,amount,batch_no,inbound_time) VALUES(1,1,1,1,1,1,10,10,'B1',CURRENT_TIMESTAMP)");
                },
                () -> basic.updateProduct(1L, edit), "basic-data-in-use");
        assertEquals(1L, basic.productDetail(1L).getBaseUnitId());
    }

    @Test
    void concurrentCategoryMovesCannotCreateACycle() throws Exception {
        jdbc.update("INSERT INTO base_product_category(id,category_name) VALUES(2,'油品')");
        CategorySaveRequest first = new CategorySaveRequest(); first.setCategoryName("粮食"); first.setParentId(2L);
        CategorySaveRequest second = new CategorySaveRequest(); second.setCategoryName("油品"); second.setParentId(1L);
        assertMutationWaitsAndRejects(() -> basic.updateCategory(1L, first), () -> {},
                () -> basic.updateCategory(2L, second), "category-cycle");
        assertEquals(2L, basic.categoryDetail(1L).getParentId());
        assertEquals(0L, basic.categoryDetail(2L).getParentId());
    }

    private void assertMutationWaitsAndRejects(Runnable validate, Runnable commit, Runnable mutation, String message) throws Exception {
        CountDownLatch validated = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch mutationStarted = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> business = workers.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(tx -> {
                validate.run(); validated.countDown(); await(release); commit.run();
            }));
            assertTrue(validated.await(5, TimeUnit.SECONDS));
            Future<?> other = workers.submit(() -> { mutationStarted.countDown(); mutation.run(); });
            assertTrue(mutationStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> other.get(300, TimeUnit.MILLISECONDS));
            release.countDown(); business.get(5, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class, () -> other.get(5, TimeUnit.SECONDS));
            assertInstanceOf(BusinessException.class, failure.getCause());
            assertEquals(message, failure.getCause().getMessage());
        } finally {
            release.countDown(); workers.shutdownNow();
            assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("business transaction was not released");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
