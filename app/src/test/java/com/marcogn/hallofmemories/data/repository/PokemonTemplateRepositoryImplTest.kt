package com.marcogn.hallofmemories.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.data.local.HallOfMemoriesDatabase
import com.marcogn.hallofmemories.data.local.entity.HackEntity
import com.marcogn.hallofmemories.data.local.entity.HallOfFameEntryEntity
import com.marcogn.hallofmemories.data.local.entity.PokemonSlotEntity
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class PokemonTemplateRepositoryImplTest {

    private lateinit var database: HallOfMemoriesDatabase
    private lateinit var repository: PokemonTemplateRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HallOfMemoriesDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = PokemonTemplateRepositoryImpl(database.pokemonTemplateDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun template(id: String = "template-1", label: String = "Competitive Garchomp") = PokemonTemplate(
        id = id,
        label = label,
        speciesId = 445,
        speciesName = "garchomp",
        nickname = null,
        gender = PokemonGender.FEMALE,
        level = 100,
        nature = "jolly",
        ability = "rough-skin",
        isShiny = false,
        heldItem = "choice-scarf",
        ivHp = 31, ivAtk = 31, ivDef = 31, ivSpAtk = 0, ivSpDef = 31, ivSpe = 31,
        evHp = 0, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 4, evSpe = 252,
        move1 = "earthquake", move2 = "dragon-claw", move3 = "stone-edge", move4 = "swords-dance",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `upsert then getById returns the saved template`() = runTest {
        repository.upsert(template())

        assertEquals(template(), repository.getById("template-1"))
    }

    @Test
    fun `observeAll reports every saved template`() = runTest {
        repository.upsert(template(id = "template-1", label = "First"))
        repository.upsert(template(id = "template-2", label = "Second"))

        val all = repository.observeAll().first()

        assertEquals(setOf("template-1", "template-2"), all.map { it.id }.toSet())
    }

    @Test
    fun `re-upserting the same id replaces it rather than duplicating it`() = runTest {
        repository.upsert(template(id = "template-1", label = "First"))
        repository.upsert(template(id = "template-1", label = "Renamed"))

        val all = repository.observeAll().first()

        assertEquals(1, all.size)
        assertEquals("Renamed", all.first().label)
    }

    @Test
    fun `deleteById removes the template`() = runTest {
        repository.upsert(template())

        repository.deleteById("template-1")

        assertNull(repository.getById("template-1"))
    }

    @Test
    fun `deleting a template a slot references leaves the slot untouched, no foreign key`() = runTest {
        repository.upsert(template())
        database.hackDao().upsert(
            HackEntity(
                id = "hack-1", name = "Test Hack", generation = GameGeneration.RSE, baseGameTitle = null,
                boxArtPath = null, boxArtUrl = null, logoPath = null, logoUrl = null, theGamesDbId = null,
                notes = null, createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
            ),
        )
        database.hallOfFameDao().saveEntryWithSlots(
            HallOfFameEntryEntity(
                id = "entry-1", hackId = "hack-1", playerName = "Ash", playerId = "1", playtimeText = "1:00",
                playtimeMinutes = 60, screenshotPath = null, insertedAt = Instant.EPOCH, notes = null,
                createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
            ),
            (0..5).map { index ->
                PokemonSlotEntity(
                    id = "slot-$index", entryId = "entry-1", slotIndex = index,
                    speciesId = if (index == 0) 445 else null, speciesName = if (index == 0) "garchomp" else null,
                    nickname = null, gender = PokemonGender.UNKNOWN, level = null, nature = null, ability = null,
                    isShiny = false, heldItem = null,
                    ivHp = null, ivAtk = null, ivDef = null, ivSpAtk = null, ivSpDef = null, ivSpe = null,
                    evHp = null, evAtk = null, evDef = null, evSpAtk = null, evSpDef = null, evSpe = null,
                    move1 = null, move2 = null, move3 = null, move4 = null,
                    sourceTemplateId = if (index == 0) "template-1" else null,
                )
            },
        )

        repository.deleteById("template-1")

        val saved = database.hallOfFameDao().observeById("entry-1").first()
        assertEquals("template-1", saved?.slots?.first { it.slotIndex == 0 }?.sourceTemplateId)
        assertNull(repository.getById("template-1"))
    }
}
