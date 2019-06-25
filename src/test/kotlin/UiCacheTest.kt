import io.mobthink.uicache.CacheHolder
import io.mobthink.uicache.CacheStorageProvider
import io.mobthink.uicache.NoCache
import io.mobthink.uicache.UiCache
import com.google.gson.Gson
import io.mockk.*
import io.reactivex.Observable
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*
import kotlin.random.Random


class UiCacheTest {

    private val key = "___KEY___"

    private val storage = mockk<CacheStorageProvider>(relaxed = true)
    private val uiCache = spyk(UiCache(TestCacheObject::class.java, storage))
    private val onDataCallback: (obj: TestCacheObject, fromCache: Boolean) -> Unit = mockk(relaxed = true)
    private val onCompleteCallback: () -> Unit = mockk(relaxed = true)

    private val gson = Gson()
    private val random = Random(System.currentTimeMillis())
    private val testCacheObject = TestCacheObject(random.nextLong(), UUID.randomUUID().toString(), random.nextFloat())

    @Before
    fun setUp() {
    }

    @Test
    fun testGetValidCache() {

        val objSlot = slot<TestCacheObject>()
        val booleanSlot = slot<Boolean>()

        every { onDataCallback(capture(objSlot), capture(booleanSlot)) } answers { nothing }
        every { onCompleteCallback() } answers { nothing }
        every { storage.exists(any()) } returns true
        every { storage.getCache(any()) } returns CacheHolder(
            key,
            gson.toJson(testCacheObject),
            Long.MAX_VALUE
        )

        uiCache.forRequest(Observable.just(testCacheObject))
            .onData(onDataCallback)
            .onRequestError { }
            .onCompleted(onCompleteCallback)
            .start()

        verify(exactly = 1) {
            onDataCallback(any(), any())
            onCompleteCallback()
        }
        Assert.assertEquals(testCacheObject, objSlot.captured)
        Assert.assertTrue(booleanSlot.captured)
    }

    @Test
    fun testGetNoValidCache() {

        val slotHolder = slot<CacheHolder>()
        val objSlot = mutableListOf<TestCacheObject>()
        val booleanSlot = mutableListOf<Boolean>()
        val networkObject = TestCacheObject(random.nextLong(), UUID.randomUUID().toString(), random.nextFloat())

        every { onDataCallback(capture(objSlot), capture(booleanSlot)) } answers { nothing }
        every { onCompleteCallback() } answers { nothing }
        every { storage.exists(any()) } returns true
        every { storage.getCache(any()) } returns CacheHolder(key, gson.toJson(testCacheObject), -1)

        uiCache.forRequest(Observable.just(networkObject))
            .onData(onDataCallback)
            .onRequestError { }
            .onCompleted(onCompleteCallback)
            .start()

        verify { storage.setCache(capture(slotHolder)) }
        verify(exactly = 2) { onDataCallback(any(), any()) }
        verify(exactly = 1) { onCompleteCallback() }
        Assert.assertEquals(gson.toJson(networkObject), slotHolder.captured.cacheData)
        Assert.assertEquals(testCacheObject, objSlot[0])
        Assert.assertTrue(booleanSlot[0])
        Assert.assertEquals(networkObject, objSlot[1])
        Assert.assertFalse(booleanSlot[1])
    }

    @Test
    fun testGetValidCacheIgnored() {

        val objSlot = slot<TestCacheObject>()
        val booleanSlot = slot<Boolean>()
        val networkObject = TestCacheObject(random.nextLong(), UUID.randomUUID().toString(), random.nextFloat())

        every { onDataCallback(capture(objSlot), capture(booleanSlot)) } answers { nothing }
        every { onCompleteCallback() } answers { nothing }
        every { storage.exists(any()) } returns true
        every { storage.getCache(any()) } returns CacheHolder(
            key,
            gson.toJson(testCacheObject),
            Long.MAX_VALUE
        )

        uiCache.forRequest(Observable.just(networkObject))
            .onData(onDataCallback)
            .onRequestError { }
            .onCompleted(onCompleteCallback)
            .start(false)

        verify(exactly = 1) {
            onDataCallback(any(), any())
            onCompleteCallback()
        }
        Assert.assertEquals(networkObject, objSlot.captured)
        Assert.assertFalse(booleanSlot.captured)
    }

    @Test
    fun testGetValidCacheWithRequestError() {

        val objSlot = slot<TestCacheObject>()
        val booleanSlot = slot<Boolean>()

        val onErrorCallback: (e: Throwable) -> Unit = mockk(relaxed = true)

        every { onDataCallback(capture(objSlot), capture(booleanSlot)) } answers { nothing }
        every { onCompleteCallback() } answers { nothing }
        every { storage.exists(any()) } returns true
        every { storage.getCache(any()) } returns CacheHolder(key, gson.toJson(testCacheObject), -1)

        uiCache.forRequest(Observable.error(Exception()))
            .onData(onDataCallback)
            .onRequestError(onErrorCallback)
            .onCompleted(onCompleteCallback)
            .start()

        verify(exactly = 1) {
            onDataCallback(any(), any())
            onErrorCallback(any())
            onCompleteCallback()
        }
        Assert.assertEquals(testCacheObject, objSlot.captured)
        Assert.assertTrue(booleanSlot.captured)
    }

    @Test
    fun testWithoutCacheRequest() {

        val objSlot = slot<TestCacheObject>()
        val booleanSlot = slot<Boolean>()
        val onNotCachedCallback: () -> Unit = mockk(relaxed = true)

        every { onDataCallback(capture(objSlot), capture(booleanSlot)) } answers { nothing }
        every { onCompleteCallback() } answers { nothing }
        every { storage.exists(any()) } returns false
        every { onNotCachedCallback() } answers { nothing }

        uiCache.forRequest(Observable.just(testCacheObject))
            .onData(onDataCallback)
            .onRequestError { }
            .onNotCached(onNotCachedCallback)
            .onCompleted(onCompleteCallback)
            .start()

        verify(exactly = 1) {
            onNotCachedCallback()
            onDataCallback(any(), any())
            onCompleteCallback()
        }
        Assert.assertEquals(testCacheObject, objSlot.captured)
        Assert.assertFalse(booleanSlot.captured)
    }

    @Test
    fun testGetValidCacheIgnoredByCustomValidator() {

        val objSlot = slot<TestCacheObject>()
        val booleanSlot = slot<Boolean>()
        val networkObject = TestCacheObject(random.nextLong(), UUID.randomUUID().toString(), random.nextFloat())

        every { onDataCallback(capture(objSlot), capture(booleanSlot)) } answers { nothing }
        every { onCompleteCallback() } answers { nothing }
        every { storage.exists(any()) } returns true
        every { storage.getCache(any()) } returns CacheHolder(
            key,
            gson.toJson(testCacheObject),
            Long.MAX_VALUE
        )

        uiCache.forRequest(Observable.just(networkObject))
            .onData(onDataCallback)
            .withCacheValidator { false }
            .onRequestError { }
            .onCompleted(onCompleteCallback)
            .start()

        verify(exactly = 1) {
            onDataCallback(any(), any())
            onCompleteCallback()
        }
        Assert.assertEquals(networkObject, objSlot.captured)
        Assert.assertFalse(booleanSlot.captured)
    }

    @Test
    fun testClassId() {

        val keySlot = slot<String>()

        every { storage.exists(any()) } returns true
        every { storage.getCache(capture(keySlot)) } returns CacheHolder(
            key,
            gson.toJson(testCacheObject),
            Long.MAX_VALUE
        )

        uiCache.forRequest(Observable.just(testCacheObject))
            .onData { _: TestCacheObject, _: Boolean -> }
            .onRequestError { }
            .start()

        Assert.assertEquals(TestCacheObject::class.java.name, keySlot.captured)
    }

    @Test
    fun testCustomId() {

        val uiCache = UiCache(TestCacheObject::class.java, storage, key)

        val keySlot = slot<String>()
        every { storage.exists(any()) } returns true
        every { storage.getCache(capture(keySlot)) } returns CacheHolder(
            key,
            gson.toJson(testCacheObject),
            Long.MAX_VALUE
        )

        uiCache.forRequest(Observable.just(testCacheObject))
            .onData { _: TestCacheObject, _: Boolean -> }
            .onRequestError { }
            .start()

        Assert.assertEquals(key, keySlot.captured)
    }

    @Test
    fun testValidity() {

        val slot = slot<CacheHolder>()
        every { storage.exists(any()) } returns false
        every { storage.setCache(capture(slot)) } answers { nothing }

        uiCache.forRequest(Observable.just(testCacheObject))
            .onData { _: TestCacheObject, _: Boolean -> }
            .onRequestError { }
            .withValidity(1000)
            .start()

        //error range, mockk does not mock native methods like System.currentTimeMillis()
        Assert.assertTrue(slot.captured.validity - System.currentTimeMillis() in 990..1000)
    }

    data class TestCacheObject(
        val id: Long,
        val name: String,
        val value: Float,
        @NoCache
        val noCacheField: String? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestCacheObject

            if (id != other.id) return false
            if (name != other.name) return false
            if (value != other.value) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }
    }
}