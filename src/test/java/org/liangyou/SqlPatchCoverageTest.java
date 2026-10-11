package org.liangyou;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlPatchCoverageTest {

    @Test
    void softDeletePatchAddsInventoryDeletedColumns() throws IOException {
        String patch = Files.readString(
                Path.of("sql/grain_oil_ims_patch_2026_04_16_soft_delete_columns.sql"),
                StandardCharsets.UTF_8);

        assertTrue(patch.contains("TABLE_NAME = 'inv_stock'"));
        assertTrue(patch.contains("ALTER TABLE `inv_stock` ADD COLUMN `deleted`"));
        assertTrue(patch.contains("TABLE_NAME = 'inv_batch'"));
        assertTrue(patch.contains("ALTER TABLE `inv_batch` ADD COLUMN `deleted`"));
    }
}
