package org.checkout.repository;

import java.util.HashSet;
import java.util.Set;

/**
 * In-memory implementation of IdempotentKeyStore.
 * Note: This in-memory implementation is for demonstration purposes only.
 * In a production environment, this should be replaced with a persistent, 
 * durable data store (e.g., Redis, Database) that includes:
 * - Configurable key expiration/eviction strategy
 * - Persistence across application restarts
 */
public class IdempotentKeyStoreRepository implements IdempotentKeyStore {

    private final Set<String> idempotencyKeys = new HashSet<>();

    public boolean contains(String key) {
        return idempotencyKeys.contains(key);
    }

    public void add(String key) {
        idempotencyKeys.add(key);
    }
}
