package io.hsar.mapgenerator.random

import java.math.BigDecimal
import java.util.*

/**
 * https://en.wikipedia.org/wiki/Military_Grid_Reference_System
 */
object GridGenerator {
    fun randomGridZone(random: Random = Random()): String {
        val utmZone = RandomGenerator.generateUniform(1, 60, random)
        val latBand = VALID_LATITUDE_BANDS.elementAt(random.nextInt(VALID_LATITUDE_BANDS.size))
        return "$utmZone$latBand"
    }

    fun Int.padZeroes(length: Int) = this.toString().padStart(length, '0')

    fun Double.padZeroes(length: Int): String {
        val normalized = BigDecimal.valueOf(this).stripTrailingZeros().toPlainString()
        val negative = normalized.startsWith('-')
        val parts = normalized.removePrefix("-").split('.', limit = 2)
        val integer = parts.first().padStart(length, '0')
        return if (parts.size == 1) {
            if (negative) "-$integer" else integer
        } else {
            val fraction = parts[1].trimEnd('0')
            val formatted = buildString {
                if (negative) append('-')
                append(integer)
                if (fraction.isNotBlank()) {
                    append('.').append(fraction)
                }
            }
            formatted
        }
    }

    fun random100kmGSID(random: Random = Random()): String =
        "${VALID_100KM_COLUMN_IDS.elementAt(random.nextInt(VALID_100KM_COLUMN_IDS.size))}${VALID_100KM_ROW_IDS.elementAt(random.nextInt(VALID_100KM_ROW_IDS.size))}"

    fun randomMGRSMapId(random: Random = Random()): String =
        "${randomGridZone(random)} ${random100kmGSID(random)}"

    val VALID_LATITUDE_BANDS = ('C'..'X').toSet() - setOf('I', 'O')
    val VALID_100KM_COLUMN_IDS = ('A'..'Z').toSet() - setOf('I', 'O')
    val VALID_100KM_ROW_IDS = ('A'..'V').toSet() - setOf('I', 'O')
}
