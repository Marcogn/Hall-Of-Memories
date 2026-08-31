package com.marcogn.hallofmemories.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.data.image.ImageStorage
import com.marcogn.hallofmemories.data.local.HallOfMemoriesDatabase
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
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
class BackupRepositoryImplTest {

    private lateinit var database: HallOfMemoriesDatabase
    private lateinit var hackRepository: HackRepositoryImpl
    private lateinit var hallOfFameRepository: HallOfFameRepositoryImpl
    private lateinit var templateRepository: PokemonTemplateRepositoryImpl
    private lateinit var imageStorage: ImageStorage
    private lateinit var backupRepository: BackupRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            HallOfMemoriesDatabase::class.java,
        ).allowMainThreadQueries().build()
        hackRepository = HackRepositoryImpl(database.hackDao())
        hallOfFameRepository = HallOfFameRepositoryImpl(database.hallOfFameDao())
        templateRepository = PokemonTemplateRepositoryImpl(database.pokemonTemplateDao())
        imageStorage = ImageStorage(ApplicationProvider.getApplicationContext())
        backupRepository = BackupRepositoryImpl(hackRepository, hallOfFameRepository, templateRepository, imageStorage, database.backupDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun emptySlot(entryId: String, index: Int) = PokemonSlot.empty(id = "$entryId-slot-$index", entryId = entryId, slotIndex = index)

    private suspend fun seedLibrary() {
        val boxArtPath = imageStorage.persistBytes(byteArrayOf(1, 2, 3))
        val screenshotPath = imageStorage.persistBytes(byteArrayOf(4, 5, 6))

        hackRepository.upsert(
            Hack(
                id = "hack-1", name = "Radical Red", generation = GameGeneration.FRLG, baseGameTitle = "Pokémon FireRed",
                boxArtPath = boxArtPath, boxArtUrl = "https://example.com/box.jpg", logoPath = null, logoUrl = null,
                theGamesDbId = 1L, notes = null, createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
            ),
        )
        templateRepository.upsert(
            PokemonTemplate(
                id = "template-1", label = "Competitive Garchomp", speciesId = 445, speciesName = "garchomp", nickname = null,
                gender = PokemonGender.FEMALE, level = 100, nature = "jolly", ability = "rough-skin", isShiny = false,
                heldItem = "choice-scarf",
                ivHp = 31, ivAtk = 31, ivDef = 31, ivSpAtk = 0, ivSpDef = 31, ivSpe = 31,
                evHp = 0, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 4, evSpe = 252,
                move1 = "earthquake", move2 = "dragon-claw", move3 = "stone-edge", move4 = "swords-dance",
                createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
            ),
        )
        val slots = listOf(
            emptySlot("entry-1", 0).copy(
                speciesId = 445, speciesName = "garchomp", level = 100, sourceTemplateId = "template-1",
            ),
        ) + (1..5).map { emptySlot("entry-1", it) }
        hallOfFameRepository.save(
            HallOfFameEntry(
                id = "entry-1", hackId = "hack-1", playerName = "Ash", playerId = "12345", playtimeText = "42:17",
                playtimeMinutes = 2537, screenshotPath = screenshotPath, insertedAt = Instant.ofEpochMilli(3_000),
                notes = "First playthrough", createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
            ),
            slots,
        )
    }

    @Test
    fun `export then import restores identical hacks, entries, slots and templates`() = runTest {
        seedLibrary()

        val payload = backupRepository.exportPayload()
        assertEquals(1, payload.hacks.size)
        assertEquals(1, payload.templates.size)

        // Simulate other things having happened to the library since the export.
        hackRepository.upsert(
            Hack(
                id = "hack-2", name = "Something Else", generation = GameGeneration.OTHER, baseGameTitle = null,
                boxArtPath = null, boxArtUrl = null, logoPath = null, logoUrl = null, theGamesDbId = null, notes = null,
                createdAt = Instant.EPOCH, updatedAt = Instant.EPOCH,
            ),
        )

        val filesByName = imageStorage.listAll().associateBy { it.name }
        val images = mutableMapOf<String, ByteArray>()
        payload.hacks.forEach { hackDto ->
            hackDto.boxArtFileName?.let { name -> images[name] = filesByName.getValue(name).readBytes() }
            hackDto.entries.forEach { entry ->
                entry.screenshotFileName?.let { name -> images[name] = filesByName.getValue(name).readBytes() }
            }
        }

        val result = backupRepository.importPayload(payload, images)

        assertEquals(1, result.hacksImported)
        assertEquals(1, result.entriesImported)
        assertEquals(1, result.templatesImported)
        assertEquals(0, result.imagesSkipped)

        // The hack created after the export must be gone — a restore is a full replace.
        assertNull(hackRepository.observeById("hack-2").first())

        val restoredHack = hackRepository.observeById("hack-1").first()
        assertEquals("Radical Red", restoredHack?.name)
        assertEquals(Instant.ofEpochMilli(1_000), restoredHack?.createdAt)

        val restoredEntries = hallOfFameRepository.observeByHack("hack-1").first()
        assertEquals(1, restoredEntries.size)
        assertEquals("Ash", restoredEntries.first().entry.playerName)
        assertEquals(6, restoredEntries.first().slots.size)
        assertEquals("template-1", restoredEntries.first().slots.first { it.slotIndex == 0 }.sourceTemplateId)

        val restoredTemplates = templateRepository.observeAll().first()
        assertEquals(1, restoredTemplates.size)
        assertEquals("Competitive Garchomp", restoredTemplates.first().label)
    }

    @Test
    fun `an image missing from the archive imports the row with a null path and is counted as skipped`() = runTest {
        seedLibrary()
        val payload = backupRepository.exportPayload()

        val result = backupRepository.importPayload(payload, images = emptyMap())

        assertEquals(2, result.imagesSkipped) // box art + screenshot, both missing from the (empty) archive
        assertNull(hackRepository.observeById("hack-1").first()?.boxArtPath)
    }
}
