package org.liangyou.modules.basic.service;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Opt-in test: real private MinIO, isolated H2 schema, no application database access. */
@EnabledIfEnvironmentVariable(named = "MINIO_SMOKE_ENABLED", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:product-images;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.cloud.nacos.config.enabled=false", "spring.config.import=", "security.jwt.secret=integration-secret",
        // Test resources shadow application.yml, so explicitly bind the opt-in storage environment.
        "app.storage.endpoint=${MINIO_ENDPOINT}", "app.storage.bucket=${MINIO_BUCKET}",
        "app.storage.access-key=${MINIO_ACCESS_KEY}", "app.storage.secret-key=${MINIO_SECRET_KEY}"
})
@ActiveProfiles("integration")
class ProductImageStorageIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductImageService images;
    @Autowired PlatformTransactionManager transactions;
    @Autowired Environment environment;
    private MinioClient storage;
    private String bucket;
    private final List<String> createdKeys = new ArrayList<>();

    @BeforeEach
    void isolatedProductSchema() throws Exception {
        storage = MinioClient.builder().endpoint(environment.getRequiredProperty("app.storage.endpoint"))
                .credentials(environment.getRequiredProperty("app.storage.access-key"),
                        environment.getRequiredProperty("app.storage.secret-key")).build();
        bucket = environment.getRequiredProperty("app.storage.bucket");
        jdbc.execute("DROP ALL OBJECTS");
        String init = Files.readString(Path.of("sql/grain_oil_ims_init.sql"));
        String ddl = init.substring(init.indexOf("CREATE TABLE base_product ("), init.indexOf("CREATE TABLE base_product_unit_convert ("));
        try (var connection = jdbc.getDataSource().getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(ddl.getBytes(StandardCharsets.UTF_8)));
        }
        jdbc.execute("ALTER TABLE base_product ADD COLUMN default_sale_price DECIMAL(18,4)");
        jdbc.execute("ALTER TABLE base_product ADD COLUMN image_object_key VARCHAR(255)");
        jdbc.update("INSERT INTO base_product(id,product_code,product_name,category_id,base_unit_id) VALUES(100,'IMAGE-TEST','图片验证商品',1,1)");
    }

    @AfterEach
    void removeOnlyObjectsCreatedByThisTest() throws Exception {
        for (String key : createdKeys) {
            storage.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
            storage.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key + ".thumb.jpg").build());
        }
    }

    @Test
    void uploadsThumbnailReplacesImageAndCleansObjectsOnRollbackAndDelete() throws Exception {
        byte[] original = png(320, 80);
        images.upload(100L, file(original));
        String firstKey = rememberCurrentKey();
        try (var content = images.open(100L, false).stream()) {
            assertArrayEquals(original, content.readAllBytes());
        }
        try (var content = images.open(100L, true).stream()) {
            BufferedImage thumbnail = ImageIO.read(content);
            assertEquals(160, thumbnail.getWidth());
            assertEquals(40, thumbnail.getHeight());
        }

        images.upload(100L, file(png(80, 320)));
        String replacementKey = rememberCurrentKey();
        assertNotEquals(firstKey, replacementKey);
        assertImageMissing(firstKey);

        AtomicReference<String> rolledBackKey = new AtomicReference<>();
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            images.upload(100L, file(original));
            rolledBackKey.set(rememberCurrentKey());
            tx.setRollbackOnly();
        });
        assertEquals(replacementKey, currentKey());
        assertImageMissing(rolledBackKey.get());
        assertNotNull(storage.statObject(StatObjectArgs.builder().bucket(bucket).object(replacementKey).build()));

        images.delete(100L);
        assertNull(currentKey());
        assertImageMissing(replacementKey);
    }

    private String currentKey() {
        return jdbc.queryForObject("SELECT image_object_key FROM base_product WHERE id=100", String.class);
    }

    private String rememberCurrentKey() {
        String key = currentKey();
        assertNotNull(key);
        createdKeys.add(key);
        return key;
    }

    private void assertImageMissing(String key) {
        for (String object : List.of(key, key + ".thumb.jpg")) {
            ErrorResponseException error = assertThrows(ErrorResponseException.class,
                    () -> storage.statObject(StatObjectArgs.builder().bucket(bucket).object(object).build()));
            assertEquals("NoSuchKey", error.errorResponse().code());
        }
    }

    private MockMultipartFile file(byte[] bytes) {
        return new MockMultipartFile("file", "test.png", "image/png", bytes);
    }

    private byte[] png(int width, int height) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }
}
