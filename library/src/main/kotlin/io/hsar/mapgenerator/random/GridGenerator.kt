package io.hsar.mapgenerator.random

/**
 * https://en.wikipedia.org/wiki/Military_Grid_Reference_System
 */
object GridGenerator {
    fun randomGridZone(): String {
        val utmZone = RandomGenerator.generateUniform(1, 60)
        val latBand = VALID_LATITUDE_BANDS.random()
        return "$utmZone$latBand"
    }

    fun Int.padZeroes(length: Int) = this.toString().padStart(length, '0')

    fun random100kmGSID(): String = "${VALID_100KM_COLUMN_IDS.random()}${VALID_100KM_ROW_IDS.random()}"

    val VALID_LATITUDE_BANDS = ('C'..'X').toSet() - setOf('I', 'O')
    val VALID_100KM_COLUMN_IDS = ('A'..'Z').toSet() - setOf('I', 'O')
    val VALID_100KM_ROW_IDS = ('A'..'V').toSet() - setOf('I', 'O')
}