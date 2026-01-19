package io.github.paulleung93.lobbylens.util

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * A thread-safe LRU cache with time-to-live (TTL) expiration.
 * 
 * @param maxSize Maximum number of entries in the cache
 * @param ttlMillis Time-to-live in milliseconds for each entry
 */
class LruCacheWithTtl<K : Any, V : Any>(
    private val maxSize: Int,
    private val ttlMillis: Long
) {
    private data class CacheEntry<V>(
        val value: V,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMillis: Long): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMillis
        }
    }
    
    // Using LinkedHashMap with accessOrder=true for LRU behavior
    private val cache = object : LinkedHashMap<K, CacheEntry<V>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, CacheEntry<V>>?): Boolean {
            return size > maxSize
        }
    }
    
    // Lock for thread safety
    private val lock = Any()
    
    companion object {
        private const val TAG = "LruCacheWithTtl"
    }
    
    /**
     * Gets a value from the cache if it exists and is not expired.
     * @return The cached value or null if not found/expired
     */
    operator fun get(key: K): V? = synchronized(lock) {
        val entry = cache[key]
        return when {
            entry == null -> null
            entry.isExpired(ttlMillis) -> {
                Log.d(TAG, "Cache entry expired for key: $key")
                cache.remove(key)
                null
            }
            else -> entry.value
        }
    }
    
    /**
     * Puts a value into the cache.
     */
    operator fun set(key: K, value: V) = synchronized(lock) {
        cache[key] = CacheEntry(value)
        Log.d(TAG, "Cached entry for key: $key (size: ${cache.size}/$maxSize)")
    }
    
    /**
     * Checks if the cache contains a non-expired entry for the key.
     */
    fun contains(key: K): Boolean = get(key) != null
    
    /**
     * Removes an entry from the cache.
     */
    fun remove(key: K): V? = synchronized(lock) {
        cache.remove(key)?.value
    }
    
    /**
     * Clears all entries from the cache.
     */
    fun clear() = synchronized(lock) {
        cache.clear()
        Log.d(TAG, "Cache cleared")
    }
    
    /**
     * Returns the current size of the cache.
     */
    val size: Int get() = synchronized(lock) { cache.size }
    
    /**
     * Removes all expired entries from the cache.
     */
    fun evictExpired() = synchronized(lock) {
        val expired = cache.entries.filter { it.value.isExpired(ttlMillis) }.map { it.key }
        expired.forEach { cache.remove(it) }
        if (expired.isNotEmpty()) {
            Log.d(TAG, "Evicted ${expired.size} expired entries")
        }
    }
}
