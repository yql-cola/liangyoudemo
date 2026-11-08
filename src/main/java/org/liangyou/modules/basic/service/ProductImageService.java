package org.liangyou.modules.basic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.product.entity.BaseProduct;
import org.liangyou.modules.product.mapper.BaseProductMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.Semaphore;

@Service
@Slf4j
public class ProductImageService {
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final long MAX_PIXELS = 6_000_000L;
    // ImageIO expands compressed images in heap; cap concurrent decodes on a small server.
    private static final Semaphore IMAGE_SLOTS = new Semaphore(2);
    private final BaseProductMapper productMapper;
    private final String endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private MinioClient client;

    public ProductImageService(BaseProductMapper productMapper,
                               @Value("${app.storage.endpoint:http://127.0.0.1:9000}") String endpoint,
                               @Value("${app.storage.bucket:product-images}") String bucket,
                               @Value("${app.storage.access-key:}") String accessKey,
                               @Value("${app.storage.secret-key:}") String secretKey) {
        this.productMapper = productMapper;
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
    }

    public record ImageContent(InputStream stream, String contentType) {}
    private record PreparedImage(byte[] original, byte[] thumbnail, String contentType, String extension) {}

    @Transactional
    public void upload(Long productId, MultipartFile file) {
        PreparedImage image = prepare(file);
        // Fail before locking the product or scheduling cleanup when no storage request can be made.
        client();
        BaseProduct product = lockedProduct(productId);
        String previous = product.getImageObjectKey();
        String key = "products/" + productId + "/" + UUID.randomUUID() + image.extension();
        try {
            put(key, image.original(), image.contentType());
            put(thumbKey(key), image.thumbnail(), "image/jpeg");
            if (productMapper.update(null, new UpdateWrapper<BaseProduct>()
                    .eq("id", productId).set("image_object_key", key).set("image_url", null)) != 1)
                throw new BusinessException(409, "product-image-update-conflict");
            // MinIO is outside the SQL transaction: remove new objects on rollback and old ones only after commit.
            afterTransaction(() -> removeQuietly(previous), () -> removeQuietly(key));
            log.info("product image uploaded productId={} objectKey={}", productId, key);
        } catch (BusinessException ex) {
            removeQuietly(key);
            throw ex;
        } catch (Exception ex) {
            removeQuietly(key);
            log.error("product image upload failed productId={} errorType={}", productId, ex.getClass().getSimpleName(), ex);
            throw new BusinessException(503, "product-image-storage-unavailable");
        }
    }

    @Transactional
    public void delete(Long productId) {
        BaseProduct product = lockedProduct(productId);
        String previous = product.getImageObjectKey();
        // Explicit SET NULL is required; updateById skips null entity fields by default.
        if (productMapper.update(null, new UpdateWrapper<BaseProduct>()
                .eq("id", productId).set("image_object_key", null).set("image_url", null)) != 1)
            throw new BusinessException(409, "product-image-update-conflict");
        afterTransaction(() -> removeQuietly(previous), () -> {});
        log.info("product image removed productId={} hadObject={}", productId, previous != null);
    }

    public ImageContent open(Long productId, boolean thumbnail) {
        BaseProduct product = productMapper.selectById(productId);
        if (product == null || !StringUtils.hasText(product.getImageObjectKey()))
            throw new BusinessException(404, "product-image-not-found");
        String key = thumbnail ? thumbKey(product.getImageObjectKey()) : product.getImageObjectKey();
        try {
            InputStream stream = client().getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());
            return new ImageContent(stream, thumbnail || key.endsWith(".jpg") ? "image/jpeg" : "image/png");
        } catch (Exception ex) {
            log.warn("product image read failed productId={} thumbnail={} errorType={}", productId, thumbnail, ex.getClass().getSimpleName());
            throw new BusinessException(503, "product-image-storage-unavailable");
        }
    }

    private BaseProduct lockedProduct(Long id) {
        BaseProduct product = productMapper.selectOne(new LambdaQueryWrapper<BaseProduct>()
                .eq(BaseProduct::getId, id).last("FOR UPDATE"));
        if (product == null) throw new BusinessException(404, "basic-data-not-found");
        return product;
    }

    private PreparedImage prepare(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES)
            throw new BusinessException(400, "product-image-size-invalid");
        if (!IMAGE_SLOTS.tryAcquire()) throw new BusinessException(503, "product-image-processing-busy");
        try {
            byte[] source = file.getBytes();
            try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(source))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new BusinessException(400, "product-image-format-invalid");
                ImageReader reader = readers.next();
                try {
                    reader.setInput(input);
                    String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                    if (!format.equals("jpeg") && !format.equals("jpg") && !format.equals("png"))
                        throw new BusinessException(400, "product-image-format-invalid");
                    int width = reader.getWidth(0), height = reader.getHeight(0);
                    if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS)
                        throw new BusinessException(400, "product-image-dimensions-invalid");
                    BufferedImage original = reader.read(0);
                    int targetWidth = Math.max(1, Math.round(width * Math.min(160f / width, 160f / height)));
                    int targetHeight = Math.max(1, Math.round(height * Math.min(160f / width, 160f / height)));
                    BufferedImage thumb = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = thumb.createGraphics();
                    try {
                        graphics.setColor(Color.WHITE); graphics.fillRect(0, 0, targetWidth, targetHeight);
                        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        graphics.drawImage(original, 0, 0, targetWidth, targetHeight, null);
                    } finally { graphics.dispose(); }
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    if (!ImageIO.write(thumb, "jpeg", output)) throw new IOException("JPEG writer unavailable");
                    return new PreparedImage(source, output.toByteArray(), format.equals("png") ? "image/png" : "image/jpeg", format.equals("png") ? ".png" : ".jpg");
                } finally { reader.dispose(); }
            }
        } catch (BusinessException ex) { throw ex; }
        catch (IOException | RuntimeException ex) {
            log.warn("product image validation failed errorType={}", ex.getClass().getSimpleName());
            throw new BusinessException(400, "product-image-format-invalid");
        } finally { IMAGE_SLOTS.release(); }
    }

    private void put(String key, byte[] bytes, String contentType) throws Exception {
        client().putObject(PutObjectArgs.builder().bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(bytes), bytes.length, -1).contentType(contentType).build());
    }

    private synchronized MinioClient client() {
        if (client == null) {
            if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey))
                throw new BusinessException(503, "product-image-storage-unconfigured");
            client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        }
        return client;
    }

    private String thumbKey(String original) { return original + ".thumb.jpg"; }

    private void removeQuietly(String key) {
        if (!StringUtils.hasText(key)) return;
        for (String object : new String[]{key, thumbKey(key)}) {
            try { client().removeObject(RemoveObjectArgs.builder().bucket(bucket).object(object).build()); }
            catch (Exception ex) { log.warn("product image cleanup failed objectKey={} errorType={}", object, ex.getClass().getSimpleName()); }
        }
    }

    private void afterTransaction(Runnable committed, Runnable rolledBack) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) { committed.run(); return; }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) committed.run();
                else rolledBack.run();
            }
        });
    }
}
