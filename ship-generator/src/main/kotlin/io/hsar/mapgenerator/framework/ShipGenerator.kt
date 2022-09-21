package io.hsar.mapgenerator.framework

import io.hsar.mapgenerator.graph.Point
import io.hsar.mapgenerator.graph.Rectangle
import io.hsar.mapgenerator.model.Compartment
import io.hsar.mapgenerator.model.CompartmentSlice
import io.hsar.mapgenerator.model.CompartmentType
import io.hsar.mapgenerator.model.Deck
import io.hsar.mapgenerator.model.Ship
import io.hsar.mapgenerator.random.GridGenerator.padZeroes
import io.hsar.mapgenerator.random.RandomGenerator.randomGaussian
import io.hsar.mapgenerator.random.RandomGenerator.randomUniform
import io.hsar.mapgenerator.random.RectangleGenerator
import java.lang.Integer.max

class ShipGenerator(val width: Double, val height: Double) {

    fun generateShip(shipType: ShipType): Ship {
        val shipScale = shipType.compartmentScale

        val engineCompartment = shipType.baseCompartments
            .filter { it.first == CompartmentType.ENGINE }
            .sortedBy { it.second }
            .first()
            .let { (engine, size) ->
                CompartmentGenerator.generatePotentialCompartment(engine, shipScale, size)
            }

        val shipArea = shipType.sizeRange.randomGaussian()
        val shipDecks = max(engineCompartment.decks, shipType.deckRange.randomUniform())

        val initialDecks = RectangleGenerator.generateRectangleCentreOn(shipArea / shipDecks, 0.4, Point.ORIGIN)
            .let { hullRectangle ->
                (0..shipDecks).map { deckNumber ->
                    Deck("Deck $deckNumber", hullRectangle, emptyList())
                }
            }

        val emptyShip = Ship(
            name = "Test Ship",
            emptyList(),
            initialDecks,
        )

        return placeEngineCompartment(emptyShip, engineCompartment)
    }

    fun placeEngineCompartment(emptyShip: Ship, potentialEngineCompartment: PotentialCompartment): Ship {
        require(potentialEngineCompartment.compartmentType == CompartmentType.ENGINE) { "First compartment must always be the engine" }
        require(potentialEngineCompartment.decks < emptyShip.decks.size) { "Can't fit the prospective engine compartment" }

        val engineCompartment = emptyShip.createCompartment(
            CompartmentType.ENGINE,
            RectangleGenerator.generateRectangleCentreOn(
                potentialEngineCompartment.potentialSizeRange.randomGaussian(0.3)
            ),
            emptyShip.decks.takeLast(potentialEngineCompartment.decks).map { it.id }
        )

        return emptyShip.placeCompartment(engineCompartment)
    }

    fun Ship.createCompartment(
        type: CompartmentType,
        rectangle: Rectangle,
        decks: List<String>
    ): Compartment {

        val compartmentNumber = this.compartments.filter { it.type == type }.size
        return Compartment(
            name = "${type.friendlyName} (${compartmentNumber.padZeroes(2)})",
            type = type,
            rectangle = rectangle,
            decks = decks
        )
    }

    fun Ship.placeCompartment(
        compartment: Compartment
    ): Ship {
        require(this.decks.isNotEmpty())
        require(compartment.decks.isNotEmpty())

        val decksByName = this.decksByName
        val decksToModify = compartment.decks.map { decksByName[it]!! }
        val decksNotModified = this.decks - decksToModify.toSet()

        val compartmentNumber = this.compartments.filter { it.type == compartment.type }.size
        decksToModify.mapIndexed { index, currDeck ->
            val sliceToAdd = CompartmentSlice(
                id = "${compartment.type}_${compartmentNumber.padZeroes(2)}",
                name = CompartmentSlice.generateName(compartment.name, compartment.decks.size, index),
                type = compartment.type,
                shape = compartment.rectangle.shape
            )
        }
        return this.copy(decks = (decksNotModified + decksToModify).sortedBy { it.name })
    }
}