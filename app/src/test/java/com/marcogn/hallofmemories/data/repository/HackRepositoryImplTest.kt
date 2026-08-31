package com.marcogn.hallofmemories.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.data.local.HallOfMemoriesDatabase
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.PokemonGender
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class HackRepositoryImplTest {

    private lateinit var database: HallOfMemoriesDatabase
    private lateinit var repository: HackRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HallOfMemoriesDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = HackRepositoryImpl(database.hackDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun hack(id: String = "hack-1") = Hack(
        id = id,
        name = "Radical Red",
        generation = GameGeneration.FRLG,
        baseGameTitle = "Pokémon FireRed Version",
        boxArtPath = "/data/images/box.jpg",
        boxArtUrl = "https://example.com/box.jpg",
        logoPath = null,
        logoUrl = null,
        theGamesDbId = 42L,
        notes = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `upsert then observeById returns the saved hack`() = runTest {
        repository.upsert(hack())

        assertEquals(hack(), repository.observeById("hack-1").first())
    }

    @Test
    fun `upsert then observeAll reports a zero entry count for a hack with no entries`() = runTest {
        repository.upsert(hack())

        val all = repository.observeAll().first()

        assertEquals(1, all.size)
        assertEquals(0, all.first().entryCount)
    }

    @Test
    fun `observeAll reports the real entry count once entries exist`() = runTest {
        repository.upsert(hack())
        database.hallOfFameDao().saveEntryWithSlots(
            HallOfFameEntryEntity(
                id = "entry-1",
                hackId = "hack-1",
                playerName = "Ash",
                playerId = "1",
                playtimeText = "1:00",
                playtimeMinutes = 60,
                screenshotPath = null,
                insertedAt = Instant.EPOCH,
                notes = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            (0..5).map { index ->
                PokemonSlotEntity(
                    id = "slot-$index",
                    entryId = "entry-1",
                    slotIndex = index,
                    speciesId = null,
                    speciesName = null,
                    nickname = null,
                    gender = PokemonGender.UNKNOWN,
                    level = null,
                    nature = null,
                    ability = null,
                    isShiny = false,
                    heldItem = null,
                    ivHp = null, ivAtk = null, ivDef = null, ivSpAtk = null, ivSpDef = null, ivSpe = null,
                    evHp = null, evAtk = null, evDef = null, evSpAtk = null, evSpDef = null, evSpe = null,
                    move1 = null, move2 = null, move3 = null, move4 = null,
                    sourceTemplateId = null,
                )
            },
        )

        val all = repository.observeAll().first()

        assertEquals(1, all.first().entryCount)
    }

    @Test
    fun `deleteById cascades to its entries and slots`() = runTest {
        repository.upsert(hack())
        database.hallOfFameDao().saveEntryWithSlots(
            HallOfFameEntryEntity(
                id = "entry-1",
                hackId = "hack-1",
                playerName = "Ash",
                playerId = "1",
                playtimeText = "1:00",
                playtimeMinutes = 60,
                screenshotPath = null,
                insertedAt = Instant.EPOCH,
                notes = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
            (0..5).map { index ->
                PokemonSlotEntity(
                    id = "slot-$index",
                    entryId = "entry-1",
                    slotIndex = index,
                    speciesId = null,
                    speciesName = null,
                    nickname = null,
                    gender = PokemonGender.UNKNOWN,
                    level = null,
                    nature = null,
                    ability = null,
                    isShiny = false,
                    heldItem = null,
                    ivHp = null, ivAtk = null, ivDef = null, ivSpAtk = null, ivSpDef = null, ivSpe = null,
                    evHp = null, evAtk = null, evDef = null, evSpAtk = null, evSpDef = null, evSpe = null,
                    move1 = null, move2 = null, move3 = null, move4 = null,
                    sourceTemplateId = null,
                )
            },
        )

        repository.deleteById("hack-1")

        assertNull(repository.observeById("hack-1").first())
        assertTrue(database.hallOfFameDao().observeByHack("hack-1").first().isEmpty())
        assertNull(database.hallOfFameDao().observeById("entry-1").first())
    }
}
