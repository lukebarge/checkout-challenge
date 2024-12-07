package org.checkout.repository;

/**
 * Interface defining operations for managing idempotency keys to prevent duplicate request processing.
 */
public interface IdempotentKeyStore {
    
    /**
     * Checks if the given idempotency key exists in the store
     * @param key The idempotency key to check
     * @return true if the key exists, false otherwise
     */
    boolean contains(String key);

    /**
     * Adds a new idempotency key to the store
     * @param key The idempotency key to add
     */
    void add(String key);
} 