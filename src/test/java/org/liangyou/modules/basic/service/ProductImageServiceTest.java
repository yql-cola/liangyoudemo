package org.liangyou.modules.basic.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.modules.product.entity.BaseProduct;
import org.liangyou.modules.product.mapper.BaseProductMapper;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;

class ProductImageServiceTest {
    @Test
    void missingStorageCredentialsFailBeforeLockingProductOrAttemptingCleanup() throws Exception {
        BaseProductMapper products = mock(BaseProductMapper.class);
        BaseProduct product = new BaseProduct(); product.setId(1L);
        when(products.selectOne(any())).thenReturn(product);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        for (String[] credentials : new String[][]{{"", "secret"}, {"key", ""}, {" ", "secret"}}) {
            ProductImageService service = new ProductImageService(products,
                    "http://localhost:9000", "images", credentials[0], credentials[1]);

            BusinessException error = assertThrows(BusinessException.class, () -> service.upload(1L,
                    new MockMultipartFile("file", "rice.png", "image/png", bytes.toByteArray())));

            assertEquals(503, error.getCode());
            assertEquals("product-image-storage-unconfigured", error.getMessage());
        }
        verifyNoInteractions(products);
    }

    @Test
    void oversizedImagesAreRejectedBeforeProductOrStorageAccess() throws Exception {
        BaseProductMapper products = mock(BaseProductMapper.class);
        MinioClient storage = mock(MinioClient.class);
        ProductImageService service = service(products, storage);
        BusinessException size = assertThrows(BusinessException.class, () -> service.upload(1L,
                new MockMultipartFile("file", "large.png", "image/png", new byte[2 * 1024 * 1024 + 1])));
        assertEquals("product-image-size-invalid", size.getMessage());

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(3001, 2000, BufferedImage.TYPE_INT_RGB), "png", bytes);
        BusinessException dimensions = assertThrows(BusinessException.class, () -> service.upload(1L,
                new MockMultipartFile("file", "pixels.png", "image/png", bytes.toByteArray())));
        assertEquals("product-image-dimensions-invalid", dimensions.getMessage());
        verifyNoInteractions(products, storage);
    }

    @Test
    void invalidImageNeverWritesProductOrStorage() {
        BaseProductMapper products = mock(BaseProductMapper.class);
        MinioClient storage = mock(MinioClient.class);
        ProductImageService service = service(products, storage);

        BusinessException error = assertThrows(BusinessException.class, () -> service.upload(1L,
                new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes())));

        assertEquals("product-image-format-invalid", error.getMessage());
        verifyNoInteractions(products, storage);
    }

    @Test
    void validImageWritesOriginalAndThumbnailBeforeProductReference() throws Exception {
        BaseProductMapper products = mock(BaseProductMapper.class);
        MinioClient storage = mock(MinioClient.class);
        BaseProduct product = new BaseProduct(); product.setId(1L);
        when(products.selectOne(any())).thenReturn(product);
        when(products.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);

        service(products, storage).upload(1L,
                new MockMultipartFile("file", "rice.png", "image/png", bytes.toByteArray()));

        verify(storage, times(2)).putObject(any(PutObjectArgs.class));
        verify(products).update(eq(null), any(UpdateWrapper.class));
    }

    @Test
    void deleteExplicitlyUpdatesNullableImageColumns() {
        BaseProductMapper products = mock(BaseProductMapper.class);
        BaseProduct product = new BaseProduct(); product.setId(1L); product.setImageObjectKey("products/1/old.jpg");
        product.setImageUrl("https://legacy.example/image.jpg");
        when(products.selectOne(any())).thenReturn(product);
        when(products.update(eq(null), any(UpdateWrapper.class))).thenReturn(1);

        service(products, mock(MinioClient.class)).delete(1L);

        verify(products).update(eq(null), argThat(wrapper -> wrapper.getSqlSet().contains("image_object_key")
                && wrapper.getSqlSet().contains("image_url")));
    }

    private ProductImageService service(BaseProductMapper products, MinioClient storage) {
        ProductImageService service = new ProductImageService(products, "http://localhost:9000", "images", "key", "secret");
        ReflectionTestUtils.setField(service, "client", storage);
        return service;
    }
}
