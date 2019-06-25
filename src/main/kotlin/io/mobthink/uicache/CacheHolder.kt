package io.mobthink.uicache

data class CacheHolder(
    val key: String,
    val cacheData: String,
    val validity: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CacheHolder

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }
}