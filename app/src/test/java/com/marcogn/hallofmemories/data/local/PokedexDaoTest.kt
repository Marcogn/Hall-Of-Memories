package com.marcogn.hallofmemories.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.marcogn.hallofmemories.data.local.entity.PokeCacheMetaEntity
import com.marcogn.hallofmemories.data.local.entity.PokeSpeciesEntity
import java.time.Instant
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
class PokedexDaoTest {

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

    private fun species(id: Int, name: String) = PokeSpeciesEntity(
        id = id,
        name = name,
        displayName = name,
        searchName = name,
        generationIntroduced = null,
        primaryType = null,
        secondaryType = null,
    )

    @Test
    fun `replaceAllSpecies is atomic - old rows are gone, new rows are all present`() = runTest {
        database.pokedexDao().replaceAllSpecies(listOf(species(1, "bulbasaur"), species(4, "charmander")))
        database.pokedexDao().replaceAllSpecies(listOf(species(25, "pikachu")))

        assertEquals(1, database.pokedexDao().countSpecies())
        assertEquals("pikachu", database.pokedexDao().getSpeciesById(25)?.name)
        assertNull(database.pokedexDao().getSpeciesById(1))
    }

    @Test
    fun `replaceAllSpecies is idempotent - running it twice with the same data leaves the same rows`() = runTest {
        val entities = listOf(species(1, "bulbasaur"), species(4, "charmander"))
        database.pokedexDao().replaceAllSpecies(entities)
        database.pokedexDao().replaceAllSpecies(entities)

        assertEquals(2, database.pokedexDao().countSpecies())
    }

    @Test
    fun `searchSpeciesByPrefix orders shorter names first`() = runTest {
        database.pokedexDao().replaceAllSpecies(
            listOf(species(1, "pikachu"), species(2, "pi"), species(3, "pidgey")),
        )

        val results = database.pokedexDao().searchSpeciesByPrefix("pi", limit = 10)

        assertEquals(listOf("pi", "pidgey", "pikachu"), results.map { it.name })
    }

    @Test
    fun `searchSpeciesByContains finds a match in the middle of the name`() = runTest {
        database.pokedexDao().replaceAllSpecies(listOf(species(122, "mrmime")))

        val results = database.pokedexDao().searchSpeciesByContains("mime", limit = 10)

        assertEquals(listOf("mrmime"), results.map { it.name })
    }

    @Test
    fun `meta round-trips and getMeta returns null for a stage never synced`() = runTest {
        assertNull(database.pokedexDao().getMeta("SPECIES"))

        database.pokedexDao().upsertMeta(
            PokeCacheMetaEntity(key = "SPECIES", lastSyncedAt = Instant.ofEpochMilli(1_000), schemaVersion = 1, itemCount = 1351),
        )

        val meta = database.pokedexDao().getMeta("SPECIES")
        assertEquals(1351, meta?.itemCount)
        assertEquals(Instant.ofEpochMilli(1_000), meta?.lastSyncedAt)
    }

    @Test
    fun `clearAllPokedexData empties every cache table and its meta`() = runTest {
        database.pokedexDao().replaceAllSpecies(listOf(species(1, "bulbasaur")))
        database.pokedexDao().upsertMeta(
            PokeCacheMetaEntity(key = "SPECIES", lastSyncedAt = Instant.EPOCH, schemaVersion = 1, itemCount = 1),
        )

        database.pokedexDao().clearAllPokedexData()

        assertEquals(0, database.pokedexDao().countSpecies())
        assertNull(database.pokedexDao().getMeta("SPECIES"))
    }
}
