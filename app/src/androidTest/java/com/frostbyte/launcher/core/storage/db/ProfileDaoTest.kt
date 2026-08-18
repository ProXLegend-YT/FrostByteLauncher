package com.frostbyte.launcher.core.storage.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.frostbyte.launcher.core.common.Loader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test against a real (in-memory) Room database. Exists
 * specifically to verify the ORDER BY behavior in ProfileDao.observeAll(),
 * which FakeProfileDao (used in JVM unit tests) does not replicate.
 */
@RunWith(AndroidJUnit4::class)
class ProfileDaoTest {

    private lateinit var db: FrostByteDatabase
    private lateinit var dao: ProfileDao

    private fun entity(name: String, lastPlayed: Long?, isDefault: Boolean = false) = ProfileEntity(
        name = name,
        minecraftVersion = "1.21.1",
        loader = Loader.FABRIC,
        javaRuntimeVersion = 17,
        ramAllocationMb = 4096,
        jvmArguments = "",
        resolutionWidth = null,
        resolutionHeight = null,
        gameDirectory = "profiles/$name",
        lastPlayedEpochMillis = lastPlayed,
        isDefault = isDefault
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, FrostByteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.profileDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeAll_ordersRecentlyPlayedFirst_thenNullsLast_thenByName() = runTest {
        dao.insert(entity("Zebra", lastPlayed = 3000))
        dao.insert(entity("Apple", lastPlayed = null))
        dao.insert(entity("Banana", lastPlayed = 5000))
        dao.insert(entity("Cherry", lastPlayed = null))

        val ordered = dao.observeAll().first().map { it.name }

        // Banana (5000) then Zebra (3000) - most recently played first,
        // then the two never-played profiles alphabetically: Apple, Cherry.
        assertEquals(listOf("Banana", "Zebra", "Apple", "Cherry"), ordered)
    }

    @Test
    fun setDefaultFlag_clearsPreviousDefault() = runTest {
        val firstId = dao.insert(entity("First", lastPlayed = null, isDefault = true))
        val secondId = dao.insert(entity("Second", lastPlayed = null))

        dao.clearDefaultFlag()
        dao.setDefaultFlag(secondId)

        val default = dao.getDefault()
        assertEquals(secondId, default?.id)
        assertEquals(false, dao.getById(firstId)?.isDefault)
    }
}
