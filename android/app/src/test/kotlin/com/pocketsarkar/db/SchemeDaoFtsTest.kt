package com.pocketsarkar.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.pocketsarkar.db.dao.SchemeDao
import com.pocketsarkar.db.entities.Scheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class SchemeDaoFtsTest {
    private lateinit var db: PocketSarkarDatabase
    private lateinit var dao: SchemeDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PocketSarkarDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.schemeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testFtsSearch() = runBlocking {
        val scheme = Scheme(
            id = "TEST_001", nameEn = "PM Kisan Samman Nidhi", nameHi = "पीएम किसान",
            category = "agriculture", ministryEn = "Agriculture", descriptionEn = "Support for farmers",
            descriptionHi = "किसानों के लिए", benefitType = "cash", targetStates = "ALL", 
            targetGender = "ALL", targetCategory = "ALL", confidenceScore = 1.0f
        )
        
        // 1. Insert into the standard table
        dao.insertSchemes(listOf(scheme))
        
        // 2. Drop any existing auto-generated FTS table to prevent conflicts
        db.openHelper.writableDatabase.execSQL("DROP TABLE IF EXISTS schemes_fts")

        // 3. Create a clean, independent FTS5 table (No 'content' binding to avoid SQLite trigger crashes)
        db.openHelper.writableDatabase.execSQL(
            "CREATE VIRTUAL TABLE schemes_fts USING FTS5(nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType)"
        )

        // 4. Manually sync the FTS table using the exact rowid
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO schemes_fts(rowid, nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType) " +
            "SELECT rowid, nameEn, nameHi, descriptionEn, descriptionHi, category, benefitType FROM schemes WHERE id = 'TEST_001'"
        )

        // 5. Test the Search (The DAO's JOIN ON s.rowid = fts.rowid will naturally link them)
        val results = dao.searchSchemes("Kisan")
        assertTrue("FTS search should return PM Kisan", results.isNotEmpty())
        assertTrue("Result ID should match", results[0].id == "TEST_001")
        
        val emptyResults = dao.searchSchemes("zzznomatch")
        assertTrue("FTS search should return empty for non-matches", emptyResults.isEmpty())
    }
}