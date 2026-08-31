package com.marcogn.hallofmemories.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonTemplateEntity
import com.marcogn.hallofmemories.domain.model.GameGeneration
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
class HallOfFameDaoTest {

    private lateinit var database: HallOfMemoriesDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HallOfMemoriesDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertHack(id: String = "hack-1"): String {
        database.hackDao().upsert(
            HackEntity(
                id = id,
                name = "Test Hack",
                generation = GameGeneration.RSE,
                baseGameTitle = null,
                boxArtPath = null,
                boxArtUrl = null,
                logoPath = null,
                logoUrl = null,
                theGamesDbId = null,
                notes = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        )
        return id
    }

    private fun emptySlot(entryId: String, index: Int) = PokemonSlotEntity(
        id = "$entryId-slot-$index",
        entryId = entryId,
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

    private fun entry(id: String, hackId: String) = HallOfFameEntryEntity(
        id = id,
        hackId = hackId,
        playerName = "Ash",
        playerId = "1",
        playtimeText = "1:00",
        playtimeMinutes = 60,
        screenshotPath = null,
        insertedAt = Instant.EPOCH,
        notes = null,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `saveEntryWithSlots creates exactly six slot rows`() = runTest {
        val hackId = insertHack()
        val slots = (0..5).map { emptySlot("entry-1", it) }

        database.hallOfFameDao().saveEntryWithSlots(entry("entry-1", hackId), slots)

        val saved = database.hallOfFameDao().observeById("entry-1").first()
        assertEquals(6, saved?.slots?.size)
    }

    @Test
    fun `re-saving an entry replaces its slots rather than duplicating them`() = runTest {
        val hackId = insertHack()
        val original = (0..5).map { emptySlot("entry-1", it) }
        database.hallOfFameDao().saveEntryWithSlots(entry("entry-1", hackId), original)

        val updated = (0..5).map { emptySlot("entry-1", it).copy(nickname = "updated-$it") }
        database.hallOfFameDao().saveEntryWithSlots(entry("entry-1", hackId), updated)

        val saved = database.hallOfFameDao().observeById("entry-1").first()
        assertEquals(6, saved?.slots?.size)
        assertTrue(saved!!.slots.all { it.nickname?.startsWith("updated-") == true })
    }

    @Test
    fun `deleting the entry removes its slots`() = runTest {
        val hackId = insertHack()
        database.hallOfFameDao().saveEntryWithSlots(entry("entry-1", hackId), (0..5).map { emptySlot("entry-1", it) })

        database.hallOfFameDao().deleteEntry("entry-1")

        assertNull(database.hallOfFameDao().observeById("entry-1").first())
    }

    @Test
    fun `deleting a hack cascades to its entries and their slots`() = runTest {
        val hackId = insertHack()
        database.hallOfFameDao().saveEntryWithSlots(entry("entry-1", hackId), (0..5).map { emptySlot("entry-1", it) })

        database.hackDao().deleteById(hackId)

        assertTrue(database.hallOfFameDao().observeByHack(hackId).first().isEmpty())
        assertNull(database.hallOfFameDao().observeById("entry-1").first())
    }

    @Test
    fun `deleting a template that a slot references does not touch the slot`() = runTest {
        val hackId = insertHack()
        database.pokemonTemplateDao().upsert(
            PokemonTemplateEntity(
                id = "template-1",
                label = "Test Template",
                speciesId = 25,
                speciesName = "pikachu",
                nickname = null,
                gender = PokemonGender.UNKNOWN,
                level = 50,
                nature = null,
                ability = null,
                isShiny = false,
                heldItem = null,
                ivHp = null, ivAtk = null, ivDef = null, ivSpAtk = null, ivSpDef = null, ivSpe = null,
                evHp = null, evAtk = null, evDef = null, evSpAtk = null, evSpDef = null, evSpe = null,
                move1 = null, move2 = null, move3 = null, move4 = null,
                createdAt = Instant.EPOCH,
                updatedAt = Instant.EPOCH,
            ),
        )
        val slots = listOf(emptySlot("entry-1", 0).copy(sourceTemplateId = "template-1")) +
            (1..5).map { emptySlot("entry-1", it) }
        database.hallOfFameDao().saveEntryWithSlots(entry("entry-1", hackId), slots)

        database.pokemonTemplateDao().deleteById("template-1")

        val saved = database.hallOfFameDao().observeById("entry-1").first()
        assertEquals("template-1", saved?.slots?.first { it.slotIndex == 0 }?.sourceTemplateId)
    }
}
