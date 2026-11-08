package org.liangyou.security;

import org.junit.jupiter.api.Test;
import org.liangyou.common.exception.BusinessException;
import org.liangyou.config.SecurityConfig;
import org.liangyou.modules.basic.controller.ProductController;
import org.liangyou.modules.basic.service.BasicDataService;
import org.liangyou.modules.basic.service.ExcelExportService;
import org.liangyou.modules.basic.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ActiveProfiles("test")
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ProductImageHttpSecurityTest {
    @Autowired MockMvc mvc;
    @MockBean BasicDataService basicDataService;
    @MockBean ExcelExportService excelExportService;
    @MockBean ProductImageService images;
    @MockBean JwtTokenService tokens;
    @MockBean StringRedisTemplate redis;

    @Test
    void jwtAuthorizedImagesCompleteWithoutAsyncRedispatchAndCloseStorageStreams() throws Exception {
        allowToken("reader", "basic:product:list");
        byte[] bytes = { (byte) 0xff, (byte) 0xd8, 1, 2, (byte) 0xff, (byte) 0xd9 };
        for (boolean thumbnail : new boolean[]{false, true}) {
            var input = new TrackedImageStream(bytes);
            when(images.open(1L, thumbnail)).thenReturn(new ProductImageService.ImageContent(input, "image/jpeg"));

            mvc.perform(get(path(thumbnail)).header("Authorization", "Bearer reader"))
                    .andExpect(request().asyncNotStarted())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(header().string("Cache-Control", "max-age=600, private"))
                    .andExpect(content().bytes(bytes));

            assertTrue(input.closed);
        }
    }

    @Test
    void anonymousAndUnprivilegedRequestsCannotReadImages() throws Exception {
        allowToken("unprivileged", "inventory:stock:list");
        for (boolean thumbnail : new boolean[]{false, true}) {
            mvc.perform(get(path(thumbnail)))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
            mvc.perform(get(path(thumbnail)).header("Authorization", "Bearer unprivileged"))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
        }
        verifyNoInteractions(images);
    }

    @Test
    void storageFailureReturnsJsonBeforeImageResponseIsCommitted() throws Exception {
        allowToken("reader", "basic:product:view");
        when(images.open(1L, false)).thenThrow(new BusinessException(503, "product-image-storage-unavailable"));

        mvc.perform(get(path(false)).header("Authorization", "Bearer reader"))
                .andExpect(request().asyncNotStarted())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("product-image-storage-unavailable"));
    }

    private void allowToken(String token, String permission) {
        when(tokens.parseToken(token)).thenReturn(new JwtTokenService.JwtTokenClaims(
                1L, token, token, List.of(), List.of(permission), 0L, Long.MAX_VALUE));
        when(tokens.buildRevokedTokenKey(token)).thenReturn("test:revoked:" + token);
        when(redis.hasKey("test:revoked:" + token)).thenReturn(false);
    }

    private String path(boolean thumbnail) {
        return "/api/v1/basic/products/1/" + (thumbnail ? "thumbnail" : "image");
    }

    private static class TrackedImageStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackedImageStream(byte[] bytes) { super(bytes); }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
