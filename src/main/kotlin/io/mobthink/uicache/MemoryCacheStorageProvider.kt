package io.mobthink.uicache

class MemoryCacheStorageProvider : CacheStorageProvider {

    private val storage: HashMap<String, CacheHolder> by lazy {
        hashMapOf<String, CacheHolder>()
    }

    override fun exists(key: String) = storage.containsKey(key)

    override fun getCache(key: String): CacheHolder? = storage[key]

    override fun setCache(holder: CacheHolder) {
        storage[holder.key] = holder
    }

    override fun clear(key: String) {
        storage.remove(key)
    }

    override fun clearAll() {
        storage.clear()
    }
}