package io.hsar.mapgenerator.graph

import io.hsar.mapgenerator.graph.Rectangle.Companion.getBoundingBox
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class RectangleTest {

    @Test
    fun `getting corners and centre works in positive space`() {
        val original = Rectangle(Point(0.0, 0.0), Point(3.0, 6.0))

        val actualTopLeft = original.topLeft
        val actualTopRight = original.topRight
        val actualBottomRight = original.bottomRight
        val actualBottomLeft = original.bottomLeft
        val actualCentre = original.centre

        val expectedTopLeft = Point(0.0, 0.0)
        val expectedTopRight = Point(3.0, 0.0)
        val expectedBottomRight = Point(3.0, 6.0)
        val expectedBottomLeft = Point(0.0, 6.0)
        val expectedCentre = Point(1.5, 3.0)

        assertThat(actualTopLeft, equalTo(expectedTopLeft))
        assertThat(actualTopRight, equalTo(expectedTopRight))
        assertThat(actualBottomRight, equalTo(expectedBottomRight))
        assertThat(actualBottomLeft, equalTo(expectedBottomLeft))
        assertThat(actualCentre, equalTo(expectedCentre))
    }

    @Test
    fun `getting corners and centre works in negative space`() {
        val original = Rectangle(Point(-6.0, -4.0), Point(-1.0, -2.0))

        val actualTopLeft = original.topLeft
        val actualTopRight = original.topRight
        val actualBottomRight = original.bottomRight
        val actualBottomLeft = original.bottomLeft
        val actualCentre = original.centre

        val expectedTopLeft = Point(-6.0, -4.0)
        val expectedTopRight = Point(-1.0, -4.0)
        val expectedBottomRight = Point(-1.0, -2.0)
        val expectedBottomLeft = Point(-6.0, -2.0)
        val expectedCentre = Point(-3.5, -3.0)

        assertThat(actualTopLeft, equalTo(expectedTopLeft))
        assertThat(actualTopRight, equalTo(expectedTopRight))
        assertThat(actualBottomRight, equalTo(expectedBottomRight))
        assertThat(actualBottomLeft, equalTo(expectedBottomLeft))
        assertThat(actualCentre, equalTo(expectedCentre))
    }

    @Test
    fun `translation works`() {
        val original = Rectangle(Point(0.0, 0.0), Point(3.0, 6.0))

        val actual = original.translate(1.0, 2.0)

        val expected = Rectangle(Point(1.0, 2.0), Point(4.0, 8.0))
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `resize-by-topleft works`() {
        val original = Rectangle(Point(-1.0, -2.0), Point(3.0, 6.0))

        val actual = original.resizeTopLeft(factorX = 0.25, factorY = 0.5)

        val expected = Rectangle(Point(-1.0, -2.0), Point(0.0, 2.0))
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `resize-by-centre works`() {
        val original = Rectangle(Point(-1.0, -2.0), Point(3.0, 6.0))

        val actual = original.resizeCentred(factorX = 0.25, factorY = 0.5)

        val expected = Rectangle(Point(0.5, 0.0), Point(1.5, 4.0))
        assertThat(actual, equalTo(expected))
    }

    @Test
    fun `bounding box generation works`() {
        val testPoints = listOf(
            Point(1.0, 0.0),
            Point(3.0, 3.0),
            Point(6.0, 2.0),
            Point(5.0, 4.0),
        )

        val actual = testPoints.getBoundingBox()

        val expected = Rectangle(Point(1.0, 0.0), Point(6.0, 4.0))
        assertThat(actual, equalTo(expected))
    }


    @Test
    fun `bounding box clipping works`() {
        val testPoints = listOf(
            Point(1.0, 0.0),
            Point(5.0, 4.0),
        )

        val actual = testPoints.getBoundingBox(clipBox = Rectangle(Point(0.0, 0.0), Point(3.0, 3.0)))

        val expected = Rectangle(Point(1.0, 0.0), Point(3.0, 3.0))
        assertThat(actual, equalTo(expected))
    }
}