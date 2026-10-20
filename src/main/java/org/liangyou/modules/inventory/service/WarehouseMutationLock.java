package org.liangyou.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import org.liangyou.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.Arrays;
import java.util.Objects;

/** Shared database mutex, including the first stock row created in a warehouse. */
@Component
@RequiredArgsConstructor
public class WarehouseMutationLock {
    private final JdbcTemplate jdbc;

    public void lock(Long... warehouseIds) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("inventory-mutation-requires-transaction");
        }
        Arrays.stream(warehouseIds).filter(Objects::nonNull).distinct().sorted().forEach(id -> {
            var rows = jdbc.queryForList("SELECT id FROM sys_warehouse WHERE id = ? AND deleted = 0 FOR UPDATE", Long.class, id);
            if (rows.isEmpty()) throw new BusinessException(404, "basic-data-not-found");
        });
    }
}
