package io.mobthink.uicache

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable

class UiCache<T : Any>(
    private val clazz: Class<T>,
    private val cacheStorageProvider: CacheStorageProvider? = null,
    private val customKey: String = clazz.name
) {

    private val memoryCacheProvider = MemoryCacheStorageProvider()
    private val gson: Gson by createGson()
    private val disposables: CompositeDisposable by lazy { CompositeDisposable() }
    private var cacheValidity = -1L
    private var noCacheCallback: (() -> Unit)? = null
    private var cacheValidatorCallback: ((obj: T) -> Boolean)? = null
    private var completedCallback: (() -> Unit)? = null
    private lateinit var dataCallback: (obj: T, fromCache: Boolean) -> Unit
    private lateinit var requestObservable: Observable<T>
    private lateinit var requestErrorCallback: (error: Throwable) -> Unit

    private fun createGson() =
        lazy {
            GsonBuilder()
                .addSerializationExclusionStrategy(object : ExclusionStrategy {
                    override fun shouldSkipField(f: FieldAttributes) = f.getAnnotation(NoCache::class.java) != null
                    override fun shouldSkipClass(clazz: Class<*>) = false
                })
                .create()
        }

    private fun requestCache(
        cacheListener: (obj: T?, refresh: Boolean) -> Unit
    ) {
        var cachedObj: T? = null
        var refreshCache = true

        var holder: CacheHolder? = null

        if (memoryCacheProvider.exists(customKey))
            holder = memoryCacheProvider.getCache(customKey)
        else if (cacheStorageProvider?.exists(customKey) == true)
            holder = cacheStorageProvider.getCache(customKey)

        if (holder != null) {
            cachedObj = parseCacheFromStorage(clazz, holder.cacheData)
            refreshCache = holder.validity < System.currentTimeMillis()
        }
        cacheListener(cachedObj, refreshCache)
    }

    private fun parseCacheFromStorage(clazz: Class<T>, cacheData: String) = gson.fromJson(cacheData, clazz)

    private fun doDataRequest() =
        requestObservable.subscribe(
            {
                updateCache(it)
                dataCallback(it, false)
                completedCallback?.invoke()
            }, {
                requestErrorCallback(it)
                completedCallback?.invoke()
            }
        ).apply { disposables.add(this) }

    private fun updateCache(obj: T) {
        val holder = CacheHolder(
            customKey,
            gson.toJson(obj),
            System.currentTimeMillis() + cacheValidity
        )
        memoryCacheProvider.setCache(holder)
        cacheStorageProvider?.setCache(holder)
    }

    private fun checkInitialization() {
        if (!this::dataCallback.isInitialized) {
            throw Exception("dataCallback must be initialized")
        }
        if (!this::requestObservable.isInitialized) {
            throw Exception("requestObservable must be initialized")
        }
        if (!this::requestErrorCallback.isInitialized) {
            throw Exception("requestErrorCallback must be initialized")
        }
    }

    fun clear(key: String) {
        memoryCacheProvider.clear(key)
        cacheStorageProvider?.clear(key)
    }

    fun clear() {
        memoryCacheProvider.clear(customKey)
        cacheStorageProvider?.clear(customKey)
    }

    fun clearAll() {
        memoryCacheProvider.clearAll()
        cacheStorageProvider?.clearAll()
    }

    fun dispose() = disposables.clear()

    fun forRequest(observable: Observable<T>): UiCache<T> {
        requestObservable = observable
        return this
    }

    fun withValidity(validityInSeconds: Long): UiCache<T> {
        cacheValidity = validityInSeconds
        return this
    }

    fun withCacheValidator(validator: (obj: T) -> Boolean): UiCache<T> {
        cacheValidatorCallback = validator
        return this
    }

    fun onData(callback: (obj: T, fromCache: Boolean) -> Unit): UiCache<T> {
        dataCallback = callback
        return this
    }

    fun onNotCached(callback: () -> Unit): UiCache<T> {
        noCacheCallback = callback
        return this
    }

    fun onRequestError(callback: (error: Throwable) -> Unit): UiCache<T> {
        requestErrorCallback = callback
        return this
    }

    fun onCompleted(callback: () -> Unit): UiCache<T> {
        completedCallback = callback
        return this
    }

    fun start(preloadCache: Boolean = true) {
        checkInitialization()

        if (preloadCache) {
            requestCache { obj, refresh ->
                if (obj != null && cacheValidatorCallback?.invoke(obj) != false) {
                    dataCallback(obj, true)
                    if (refresh) doDataRequest()
                    else completedCallback?.invoke()
                } else {
                    noCacheCallback?.invoke()
                    doDataRequest()
                }
            }

        } else doDataRequest()
    }
}