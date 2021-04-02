package org.pharosnet.vertx.faas.database.core;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import io.vertx.sqlclient.Transaction;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransactionEvictionListener implements RemovalListener<String, CachedTransaction> {

    private static final Logger log = LoggerFactory.getLogger(TransactionEvictionListener.class);

    @Override
    public void onRemoval(@Nullable String key, @Nullable CachedTransaction value, RemovalCause cause) {
        if (!cause.wasEvicted()) {
            return;
        }
        if (value == null) {
            return;
        }
        if (log.isWarnEnabled()) {
            log.warn("cancel transaction({}) by evicted", key);
        }
        Transaction transaction = value.getTransaction();
        transaction.rollback()
                .eventually(r -> value.getConnection().close())
                .onSuccess(r -> {
                    if (log.isDebugEnabled()) {
                        log.debug("cancel transaction({}) by evicted succeed", key);
                    }
                })
                .onFailure(e -> {
                    log.error("cancel transaction({}) by evicted failed", key, e);
                });
    }
}
