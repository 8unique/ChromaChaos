package com.chromachaos.game.data.local

import androidx.compose.ui.graphics.Color
import androidx.room.TypeConverter
import com.chromachaos.game.data.model.Difficulty
import com.chromachaos.game.data.model.SpecialBlockType

class Converters {

    @TypeConverter
    fun fromDifficulty(value: Difficulty): String {
        return value.name
    }

    @TypeConverter
    fun toDifficulty(value: String): Difficulty {
        return Difficulty.valueOf(value)
    }

    @TypeConverter
    fun fromColor(color: Color?): ULong? {
        return color?.value
    }

    @TypeConverter
    fun toColor(value: Long?): Color? {
        return value?.let { Color(it) }
    }

    @TypeConverter
    fun fromSpecialBlockType(type: SpecialBlockType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toSpecialBlockType(value: String?): SpecialBlockType? {
        return value?.let { SpecialBlockType.valueOf(it) }
    }
}
