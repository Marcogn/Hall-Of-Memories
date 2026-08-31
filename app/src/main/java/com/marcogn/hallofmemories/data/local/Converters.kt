package com.marcogn.hallofmemories.data.local

import androidx.room.TypeConverter
import com.marcogn.hallofmemories.domain.model.GameGeneration
import com.marcogn.hallofmemories.domain.model.PokemonGender
import java.time.Instant

class Converters {

    @TypeConverter
    fun fromEpochMilli(epochMilli: Long?): Instant? = epochMilli?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? = instant?.toEpochMilli()

    @TypeConverter
    fun fromGenerationName(name: String): GameGeneration = GameGeneration.valueOf(name)

    @TypeConverter
    fun generationToName(generation: GameGeneration): String = generation.name

    @TypeConverter
    fun fromGenderName(name: String): PokemonGender = PokemonGender.valueOf(name)

    @TypeConverter
    fun genderToName(gender: PokemonGender): String = gender.name
}
