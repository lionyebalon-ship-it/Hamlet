package com.hamlet.core

import java.nio.charset.StandardCharsets
import java.util.UUID

data class Location(
    val id: String,
    val name: String,
    val kind: String,
)

data class Villager(
    val id: String,
    val name: String,
    val age: Int,
    val occupation: String,
    val personalityTraits: Set<String>,
    val ambition: String,
    val homeLocationId: String,
    val workplaceLocationId: String?,
    val currentLocationId: String,
    val activity: NpcActivity = NpcActivity.IDLE,
    val plannedDestinationId: String? = null,
)

enum class NpcActivity {
    IDLE,
    TRAVELING,
    WORKING,
    RESTING,
    OBSERVING,
}

data class Relationship(
    val fromVillagerId: String,
    val toVillagerId: String,
    val description: String,
    val trust: Double,
    val friendship: Double,
    val respect: Double,
    val familiarity: Double,
    val affection: Double,
    val resentment: Double,
)

data class HamletWorld(
    val id: String,
    val name: String,
    val seed: Long,
    val revision: Long,
    val locations: Map<String, Location>,
    val villagers: Map<String, Villager>,
    val relationships: List<Relationship>,
)

object WorldInitializer {
    fun initializeIfAbsent(existing: HamletWorld?, seed: Long = DEFAULT_SEED): HamletWorld =
        existing ?: createFirstHamlet(seed)

    fun createFirstHamlet(seed: Long = DEFAULT_SEED): HamletWorld {
        val locations = listOf(
            Location("town-square", "Town Square", "gathering"),
            Location("tavern", "The Golden Stag Tavern", "business"),
            Location("blacksmith", "Blacksmith", "workplace"),
            Location("bakery", "Bakery", "workplace"),
            Location("market", "Market", "business"),
            Location("farm", "Farm", "workplace"),
            Location("chapel", "Chapel", "community"),
            Location("forest", "Forest", "wilderness"),
            Location("carpenter-workshop", "Carpenter Workshop", "workplace"),
            Location("residential-area", "Residential Area", "residential"),
        ).associateBy { stableId(seed, "location:${it.id}") }

        fun locationId(key: String) = stableId(seed, "location:$key")
        fun villagerId(key: String) =
            stableId(seed, "villager:${key.lowercase().replace(" ", "-")}")

        val villagers = listOf(
            villager("Aldric", 46, "Blacksmith", setOf("proud", "disciplined", "stubborn"), "Expand his smithy", "blacksmith"),
            villager("Eliza", 34, "Baker", setOf("caring", "practical", "sociable"), "Secure reliable flour supplies", "bakery"),
            villager("Thomas", 39, "Farmer", setOf("patient", "hardworking", "cautious"), "Improve his harvest", "farm"),
            villager("Eleanor", 42, "Herbalist", setOf("curious", "compassionate", "analytical"), "Expand her herbal knowledge", "market"),
            villager("Roland", 51, "Innkeeper", setOf("sociable", "perceptive", "humorous"), "Improve the tavern", "tavern"),
            villager("Mira", 29, "Merchant", setOf("ambitious", "persuasive", "calculating"), "Expand her trading network", "market"),
            villager("Edmund", 44, "Carpenter", setOf("creative", "competitive", "independent"), "Gain recognition for his craftsmanship", "carpenter-workshop"),
            villager("Beatrice", 36, "Village Guard", setOf("principled", "observant", "protective"), "Improve village safety", "town-square"),
            villager("Father Oswin", 63, "Cleric", setOf("reflective", "patient", "forgiving"), "Strengthen the community", "chapel"),
            villager("Agnes", 26, "Seamstress", setOf("artistic", "independent", "determined"), "Establish a successful tailoring business", "market"),
            villager("Finn", 24, "Hunter", setOf("adventurous", "observant", "impulsive"), "Explore surrounding lands", "forest"),
            villager("Margaret", 71, "Retired farmer and village elder", setOf("wise", "nostalgic", "opinionated"), "Preserve village traditions", null),
        ).associate { villager ->
            villagerId(villager.name.lowercase().replace(" ", "-")) to villager.copy(
                id = villagerId(villager.name.lowercase().replace(" ", "-")),
                homeLocationId = locationId("residential-area"),
                workplaceLocationId = villager.workplaceLocationId?.let(::locationId),
                currentLocationId = locationId("residential-area"),
            )
        }

        fun relation(
            from: String,
            to: String,
            description: String,
            trust: Double,
            friendship: Double,
            respect: Double,
            familiarity: Double,
            affection: Double,
            resentment: Double = 0.0,
        ) = Relationship(
            fromVillagerId = villagerId(from),
            toVillagerId = villagerId(to),
            description = description,
            trust = trust,
            friendship = friendship,
            respect = respect,
            familiarity = familiarity,
            affection = affection,
            resentment = resentment,
        )

        val relationships = listOf(
            relation("Aldric", "Edmund", "professional rivalry", 0.42, 0.18, 0.62, 0.75, 0.12, 0.55),
            relation("Edmund", "Aldric", "professional rivalry", 0.36, 0.14, 0.68, 0.75, 0.10, 0.62),
            relation("Eliza", "Thomas", "good friends", 0.86, 0.88, 0.78, 0.82, 0.74),
            relation("Thomas", "Eliza", "good friends", 0.82, 0.84, 0.76, 0.82, 0.70),
            relation("Eliza", "Roland", "business partners", 0.76, 0.55, 0.72, 0.80, 0.42),
            relation("Roland", "Eliza", "business partners", 0.80, 0.58, 0.75, 0.80, 0.45),
            relation("Eleanor", "Margaret", "trusted friends", 0.91, 0.86, 0.89, 0.78, 0.77),
            relation("Margaret", "Eleanor", "trusted friends", 0.94, 0.89, 0.88, 0.78, 0.81),
            relation("Beatrice", "Roland", "mutual respect", 0.64, 0.42, 0.84, 0.70, 0.34),
            relation("Roland", "Beatrice", "mutual respect", 0.68, 0.46, 0.81, 0.70, 0.38),
            relation("Mira", "Finn", "trade acquaintances", 0.53, 0.28, 0.50, 0.48, 0.20),
            relation("Finn", "Mira", "trade acquaintances", 0.46, 0.24, 0.54, 0.48, 0.18),
            relation("Father Oswin", "Margaret", "longtime friends", 0.93, 0.92, 0.88, 0.95, 0.84),
            relation("Margaret", "Father Oswin", "longtime friends", 0.96, 0.94, 0.91, 0.95, 0.89),
            relation("Agnes", "Eliza", "friendly neighbors", 0.68, 0.62, 0.60, 0.55, 0.52),
            relation("Eliza", "Agnes", "friendly neighbors", 0.73, 0.67, 0.64, 0.55, 0.56),
        )

        return HamletWorld(
            id = stableId(seed, "world:first-hamlet"),
            name = "The First Hamlet",
            seed = seed,
            revision = 0,
            locations = locations,
            villagers = villagers,
            relationships = relationships,
        )
    }

    private fun villager(
        name: String,
        age: Int,
        occupation: String,
        traits: Set<String>,
        ambition: String,
        workplace: String?,
    ) = Villager(
        id = "",
        name = name,
        age = age,
        occupation = occupation,
        personalityTraits = traits,
        ambition = ambition,
        homeLocationId = "",
        workplaceLocationId = workplace,
        currentLocationId = "",
    )

    private fun stableId(seed: Long, key: String): String =
        UUID.nameUUIDFromBytes("$seed:$key".toByteArray(StandardCharsets.UTF_8)).toString()

    const val DEFAULT_SEED: Long = 120_034L
}
