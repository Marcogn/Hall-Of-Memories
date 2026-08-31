package com.marcogn.hallofmemories.domain.backup

import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.Hack
import com.marcogn.hallofmemories.domain.model.HallOfFameEntry
import com.marcogn.hallofmemories.domain.model.HallOfFameEntryWithSlots
import com.marcogn.hallofmemories.domain.model.PokemonGender
import com.marcogn.hallofmemories.domain.model.PokemonSlot
import com.marcogn.hallofmemories.domain.model.PokemonTemplate
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPayloadTest {

    private fun slot(index: Int, filled: Boolean) = if (filled) {
        PokemonSlot(
            id = "slot-$index", entryId = "entry-1", slotIndex = index, speciesId = 6, speciesName = "charizard",
            nickname = "Torchy", gender = PokemonGender.MALE, level = 100, nature = "adamant", ability = "blaze",
            isShiny = true, heldItem = "leftovers",
            ivHp = 31, ivAtk = 31, ivDef = 30, ivSpAtk = 1, ivSpDef = 29, ivSpe = 31,
            evHp = 4, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 0, evSpe = 252,
            move1 = "flamethrower", move2 = "dragon-claw", move3 = "earthquake", move4 = "roost",
            sourceTemplateId = "template-1",
        )
    } else {
        PokemonSlot.empty(id = "slot-$index", entryId = "entry-1", slotIndex = index)
    }

    private fun entryWithSlots() = HallOfFameEntryWithSlots(
        entry = HallOfFameEntry(
            id = "entry-1", hackId = "hack-1", playerName = "Ash", playerId = "12345", playtimeText = "42:17",
            playtimeMinutes = 2537, screenshotPath = "/data/images/shot.jpg", insertedAt = Instant.ofEpochMilli(3_000),
            notes = "First playthrough", createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
        ),
        slots = (0..5).map { slot(it, filled = it == 0) },
    )

    private fun hack() = Hack(
        id = "hack-1", name = "Radical Red", generation = GameGeneration.FRLG, baseGameTitle = "Pokémon FireRed Version",
        boxArtPath = "/data/images/box.jpg", boxArtUrl = "https://example.com/box.jpg",
        logoPath = "/data/images/logo.png", logoUrl = "https://example.com/logo.png",
        theGamesDbId = 12345L, notes = "A tough ROM hack",
        createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
    )

    private fun template() = PokemonTemplate(
        id = "template-1", label = "Competitive Garchomp", speciesId = 445, speciesName = "garchomp", nickname = null,
        gender = PokemonGender.FEMALE, level = 100, nature = "jolly", ability = "rough-skin", isShiny = false,
        heldItem = "choice-scarf",
        ivHp = 31, ivAtk = 31, ivDef = 31, ivSpAtk = 0, ivSpDef = 31, ivSpe = 31,
        evHp = 0, evAtk = 252, evDef = 0, evSpAtk = 0, evSpDef = 4, evSpe = 252,
        move1 = "earthquake", move2 = "dragon-claw", move3 = "stone-edge", move4 = "swords-dance",
        createdAt = Instant.ofEpochMilli(1_000), updatedAt = Instant.ofEpochMilli(2_000),
    )

    @Test
    fun `a hack with its entries and slots round-trips through JSON unchanged`() {
        val originalHack = hack()
        val originalEntry = entryWithSlots()
        val payload = BackupPayload(
            exportedAt = "2026-01-01T00:00:00Z",
            hacks = listOf(originalHack.toBackupDto(listOf(originalEntry))),
            templates = listOf(template().toBackupDto()),
        )

        val decoded = payload.toJson().toBackupPayload()

        assertEquals(1, decoded.hacks.size)
        val restoredHackDto = decoded.hacks.first()
        val restoredHack = restoredHackDto.toDomain(resolvedBoxArtPath = "/new/box.jpg", resolvedLogoPath = "/new/logo.png")
        assertEquals(originalHack.copy(boxArtPath = "/new/box.jpg", logoPath = "/new/logo.png"), restoredHack)

        assertEquals(1, restoredHackDto.entries.size)
        val restoredEntry = restoredHackDto.entries.first().toDomain(hackId = "hack-1", resolvedScreenshotPath = "/new/shot.jpg")
        assertEquals(originalEntry.entry.copy(screenshotPath = "/new/shot.jpg"), restoredEntry.entry)
        assertEquals(originalEntry.slots, restoredEntry.slots)
    }

    @Test
    fun `a template round-trips through JSON unchanged`() {
        val originalTemplate = template()
        val payload = BackupPayload(exportedAt = "2026-01-01T00:00:00Z", templates = listOf(originalTemplate.toBackupDto()))

        val decoded = payload.toJson().toBackupPayload()

        assertEquals(originalTemplate, decoded.templates.first().toDomain())
    }

    @Test
    fun `a payload JSON with no templates key still decodes to an empty list`() {
        val json = """{"formatVersion":1,"exportedAt":"2026-01-01T00:00:00Z","hacks":[]}"""

        val decoded = json.toBackupPayload()

        assertTrue(decoded.templates.isEmpty())
    }

    @Test
    fun `a formatVersion newer than this build supports is rejected with a typed error`() {
        val json = """{"formatVersion":99,"exportedAt":"2026-01-01T00:00:00Z","hacks":[],"templates":[]}"""

        val exception = assertThrows(BackupFormatTooNewException::class.java) { json.toBackupPayload() }

        assertEquals(99, exception.fileVersion)
    }

    @Test
    fun `an empty slot round-trips with every nullable field null`() {
        val emptySlot = PokemonSlot.empty(id = "slot-3", entryId = "entry-1", slotIndex = 3)

        val decoded = emptySlot.toBackupDto().toDomain(entryId = "entry-1")

        assertEquals(emptySlot, decoded)
    }
}
