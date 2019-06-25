package io.mobthink.uicache

/**
 * Basic interface for cache storage
 */
interface CacheStorageProvider {

    /**
     * A faster way to check if the cache is present on this storage
     *
     * @param key the cache key
     * @return true if cache is present with provided key
     */
    fun exists(key: String): Boolean

    /**
     * Get a cached object by key
     *
     * @param key the cache key
     * @return A CacheHolder object or null if does not exist
     */
    fun getCache(key: String): CacheHolder?

    /**
     * Save a cache object
     *
     * @param holder the object to be cached
     */
    fun setCache(holder: CacheHolder)

    /**
     * Remove the cache with the provided key
     *
     * @param key the cache key
     */
    fun clear(key: String)

    /**
     * Clear all caches
     *
     * @param key the cache key
     */
    fun clearAll()
}