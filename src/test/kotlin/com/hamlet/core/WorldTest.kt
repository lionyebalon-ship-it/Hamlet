package com.hamlet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldTest {
    @Test
    fun `first world contains the expected village`() {
        val world = WorldInitializer.createFirstHamlet()

        assertEquals("The First Hamlet", world.name)
        assertEquals(12, world.villagers.size)
        assertEquals(10, world.locations.size)
        assertEquals(16, world.relationships.size)
        assertEquals(
            setOf(
                "Aldric", "Eliza", "Thomas", "Eleanor", "Roland", "Mira",
                "Edmund", "Beatrice", "Father Oswin", "Agnes", "Finn", "Margaret",
            ),
            world.villagers.values.map { it.name }.toSet(),
        )
        assertEquals(
            setOf(
                "Town Square", "The Golden Stag Tavern", "Blacksmith", "Bakery", "Market",
                "Farm", "Chapel", "Forest", "Carpenter Workshop", "Residential Area",
            ),
            world.locations.values.map { it.name }.toSet(),
        )
        assertTrue(world.villagers.values.all { it.homeLocationId in world.locations })
        assertTrue(world.villagers.values.all {
            it.workplaceLocationId == null || it.workplaceLocationId in world.locations
        })
        assertTrue(world.relationships.all {
            it.fromVillagerId in world.villagers && it.toVillagerId in world.villagers
        })
        assertTrue(world.relationships.all { relationship ->
            listOf(
                relationship.trust,
                relationship.friendship,
                relationship.respect,
                relationship.familiarity,
                relationship.affection,
                relationship.resentment,
            ).all { it in 0.0..1.0 }
        })
        assertEquals(
            setOf(
                "Aldric>Edmund", "Edmund>Aldric",
                "Eliza>Thomas", "Thomas>Eliza",
                "Eliza>Roland", "Roland>Eliza",
                "Eleanor>Margaret", "Margaret>Eleanor",
                "Beatrice>Roland", "Roland>Beatrice",
                "Mira>Finn", "Finn>Mira",
                "Father Oswin>Margaret", "Margaret>Father Oswin",
                "Agnes>Eliza", "Eliza>Agnes",
            ),
            world.relationships.map { relationship ->
                val from = world.villagers.getValue(relationship.fromVillagerId).name
                val to = world.villagers.getValue(relationship.toVillagerId).name
                "$from>$to"
            }.toSet(),
        )
    }

    @Test
    fun `world generation is deterministic for a seed`() {
        val first = WorldInitializer.createFirstHamlet(seed = 42)
        val second = WorldInitializer.createFirstHamlet(seed = 42)
        val otherSeed = WorldInitializer.createFirstHamlet(seed = 43)

        assertEquals(first, second)
        assertNotEquals(first.id, otherSeed.id)
        assertEquals(first.villagers.values.map { it.name }, second.villagers.values.map { it.name })
    }

    @Test
    fun `initializer preserves an existing world`() {
        val existing = WorldInitializer.createFirstHamlet().copy(revision = 8)

        assertSame(existing, WorldInitializer.initializeIfAbsent(existing))
        assertEquals(8L, WorldInitializer.initializeIfAbsent(existing).revision)
    }

    @Test
    fun `initial relationships are directed and can be asymmetric`() {
        val world = WorldInitializer.createFirstHamlet()
        val aldric = world.villagers.values.single { it.name == "Aldric" }.id
        val edmund = world.villagers.values.single { it.name == "Edmund" }.id
        val aldricToEdmund = world.relationships.single {
            it.fromVillagerId == aldric && it.toVillagerId == edmund
        }
        val edmundToAldric = world.relationships.single {
            it.fromVillagerId == edmund && it.toVillagerId == aldric
        }

        assertNotEquals(aldricToEdmund.trust, edmundToAldric.trust)
    }
}
