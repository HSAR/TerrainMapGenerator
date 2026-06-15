package io.hsar.mapgenerator.random

import java.util.Random

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

    fun random100kmGSID(random: Random = Random()): String =
        "${VALID_100KM_COLUMN_IDS.elementAt(random.nextInt(VALID_100KM_COLUMN_IDS.size))}${VALID_100KM_ROW_IDS.elementAt(random.nextInt(VALID_100KM_ROW_IDS.size))}"

    val VALID_LATITUDE_BANDS = ('C'..'X').toSet() - setOf('I', 'O')
    val VALID_100KM_COLUMN_IDS = ('A'..'Z').toSet() - setOf('I', 'O')
    val VALID_100KM_ROW_IDS = ('A'..'V').toSet() - setOf('I', 'O')
}
