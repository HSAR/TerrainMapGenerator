package io.hsar.mapgenerator.map

import kotlin.math.truncate

/**
 * Reimplemented from https://myforest.uk/
 * Graham Relf, UK, 2006-2021
 */
object TerrainGenerator {
    fun generateTerrain(width: Int, height: Int): Array<DoubleArray> {
        return (0 until width).map { x ->
            (0 until height).map { y ->
                calculateHeight(x = x, y = y)
            }.toDoubleArray()
        }.toTypedArray()
    }

    private fun calculateHeight(x: Int, y: Int): Double {
        var height = 0.0
        for ((repeat_x, repeat_y) in REPEAT_PROFILES) {
            val j = (repeat_x * x + repeat_y * y) * RECIP128
            val jint = truncate(j).toInt()
            val jfrac = j - jint
            val prof0 = HEIGHT_PROFILE[jint and 0xff]
            val prof1 = HEIGHT_PROFILE[(jint + 1) and 0xff]
            height += prof0 + jfrac * (prof1 - prof0); // interpolate
        }
        return height / REPEAT_PROFILES.size
    }

    private val HEIGHT_PROFILE = arrayOf(
        0.5579710144842969,
        0.5797101449187501,
        0.6086956521646875,
        0.637681159410625,
        0.6666666666565625,
        0.6956521739025,
        0.7318840579599218,
        0.753623188394375,
        0.7826086956403125,
        0.81159420288625,
        0.8333333333207031,
        0.8550724637551562,
        0.869565217378125,
        0.8913043478125782,
        0.9130434782470312,
        0.9347826086814843,
        0.9492753623044531,
        0.9637681159274218,
        0.9710144927389063,
        0.9710144927389063,
        0.9637681159274218,
        0.9637681159274218,
        0.9492753623044531,
        0.9420289854929688,
        0.9347826086814843,
        0.9130434782470312,
        0.8913043478125782,
        0.8840579710010937,
        0.8840579710010937,
        0.8840579710010937,
        0.8913043478125782,
        0.9057971014355469,
        0.9130434782470312,
        0.9420289854929688,
        0.9710144927389063,
        0.9927536231733594,
        0.9927536231733594,
        0.9999999999848438,
        0.9999999999848438,
        0.9927536231733594,
        0.9782608695503906,
        0.9637681159274218,
        0.9347826086814843,
        0.8913043478125782,
        0.8550724637551562,
        0.8043478260747656,
        0.7608695652058594,
        0.7318840579599218,
        0.7028985507139843,
        0.6739130434680469,
        0.6521739130335937,
        0.6231884057876562,
        0.5942028985417187,
        0.5652173912957813,
        0.5362318840498438,
        0.5144927536153906,
        0.4999999999924219,
        0.48550724636945314,
        0.48550724636945314,
        0.48550724636945314,
        0.47826086955796876,
        0.48550724636945314,
        0.4999999999924219,
        0.5144927536153906,
        0.5289855072383594,
        0.5362318840498438,
        0.5289855072383594,
        0.5289855072383594,
        0.5144927536153906,
        0.4999999999924219,
        0.47826086955796876,
        0.44927536231203125,
        0.42028985506609373,
        0.3913043478201563,
        0.3768115941971875,
        0.3768115941971875,
        0.3913043478201563,
        0.39855072463164065,
        0.42028985506609373,
        0.4275362318775781,
        0.44927536231203125,
        0.4565217391235156,
        0.4565217391235156,
        0.4710144927464844,
        0.4710144927464844,
        0.4710144927464844,
        0.47826086955796876,
        0.47826086955796876,
        0.48550724636945314,
        0.4999999999924219,
        0.5072463768039063,
        0.5289855072383594,
        0.5579710144842969,
        0.5797101449187501,
        0.5942028985417187,
        0.6159420289761719,
        0.637681159410625,
        0.6521739130335937,
        0.6739130434680469,
        0.6884057970910157,
        0.6956521739025,
        0.6956521739025,
        0.6956521739025,
        0.6956521739025,
        0.6739130434680469,
        0.6666666666565625,
        0.6521739130335937,
        0.6159420289761719,
        0.5797101449187501,
        0.5434782608613281,
        0.5144927536153906,
        0.48550724636945314,
        0.4565217391235156,
        0.4347826086890625,
        0.42028985506609373,
        0.39855072463164065,
        0.3768115941971875,
        0.36231884057421876,
        0.3405797101397656,
        0.3188405797053125,
        0.3115942028938281,
        0.29710144927085935,
        0.28985507245937503,
        0.28260869564789065,
        0.2608695652134375,
        0.25362318840195314,
        0.23913043477898438,
        0.2318840579675,
        0.21739130434453124,
        0.2028985507215625,
        0.173913043475625,
        0.14492753622968751,
        0.10869565217226562,
        0.07971014492632812,
        0.05072463768039062,
        0.021739130434453125,
        0.01449275362296875,
        0.01449275362296875,
        0.01449275362296875,
        0.01449275362296875,
        0.01449275362296875,
        0.01449275362296875,
        0.021739130434453125,
        0.04347826086890625,
        0.05072463768039062,
        0.07246376811484376,
        0.07971014492632812,
        0.10869565217226562,
        0.13043478260671876,
        0.15942028985265624,
        0.173913043475625,
        0.18115942028710938,
        0.18115942028710938,
        0.18840579709859376,
        0.18840579709859376,
        0.18115942028710938,
        0.18115942028710938,
        0.18115942028710938,
        0.18115942028710938,
        0.18115942028710938,
        0.18840579709859376,
        0.2028985507215625,
        0.21014492753304687,
        0.21739130434453124,
        0.23913043477898438,
        0.2608695652134375,
        0.2681159420249219,
        0.28260869564789065,
        0.28260869564789065,
        0.28985507245937503,
        0.28985507245937503,
        0.28985507245937503,
        0.28260869564789065,
        0.28260869564789065,
        0.28260869564789065,
        0.2681159420249219,
        0.2681159420249219,
        0.2681159420249219,
        0.2608695652134375,
        0.2608695652134375,
        0.2608695652134375,
        0.25362318840195314,
        0.25362318840195314,
        0.23913043477898438,
        0.23913043477898438,
        0.2318840579675,
        0.21739130434453124,
        0.2028985507215625,
        0.18115942028710938,
        0.14492753622968751,
        0.10869565217226562,
        0.07971014492632812,
        0.07246376811484376,
        0.06521739130335938,
        0.06521739130335938,
        0.06521739130335938,
        0.06521739130335938,
        0.07971014492632812,
        0.10144927536078124,
        0.10869565217226562,
        0.12318840579523438,
        0.12318840579523438,
        0.13043478260671876,
        0.13043478260671876,
        0.13043478260671876,
        0.13043478260671876,
        0.13043478260671876,
        0.13043478260671876,
        0.12318840579523438,
        0.12318840579523438,
        0.12318840579523438,
        0.10869565217226562,
        0.10144927536078124,
        0.09420289854929688,
        0.07971014492632812,
        0.07971014492632812,
        0.07246376811484376,
        0.07246376811484376,
        0.07246376811484376,
        0.07971014492632812,
        0.09420289854929688,
        0.10144927536078124,
        0.12318840579523438,
        0.14492753622968751,
        0.15942028985265624,
        0.18115942028710938,
        0.2028985507215625,
        0.21739130434453124,
        0.25362318840195314,
        0.28260869564789065,
        0.29710144927085935,
        0.32608695651679687,
        0.36231884057421876,
        0.42028985506609373,
        0.4565217391235156,
        0.4999999999924219,
        0.5289855072383594,
        0.5579710144842969,
        0.5797101449187501,
        0.5942028985417187,
        0.6086956521646875,
        0.6086956521646875,
        0.6159420289761719,
        0.6159420289761719,
        0.6086956521646875,
        0.6086956521646875,
        0.5942028985417187,
        0.5869565217302344,
        0.5797101449187501,
        0.5434782608613281,
        0.5289855072383594,
        0.5144927536153906,
        0.5144927536153906,
        0.5289855072383594,
        0.5362318840498438,
        0.5434782608613281
    )

    private val REPEAT_PROFILES = arrayOf(0 to 27, 13 to 24, 21 to 21, 22 to 14, 29 to 3, 7 to 0)

    private val RECIP128 = 1.0 / 128.0

}