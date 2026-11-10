package com.hamlet.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDecisionsTest {
    @Test
    fun `valid AI travel proposal records a route without teleporting`() = runBlocking {
        val world = WorldInitializer.createFirstHamlet()
        val aldric = world.villagers.values.single { it.name == "Aldric" }
        lateinit var receivedContext: NpcDecisionContext
        val provider = AiDecisionProvider { context ->
            receivedContext = context
            ProviderResult.Success(
                DecisionProposal(
                    npcId = context.npcId,
                    basedOnWorldRevision = context.worldRevision,
                    reasoningSummary = "I should return to my smithy.",
                    action = ProposedAction(
                        type = ProposedActionType.TRAVEL_TO,
                        targetLocationId = context.workplaceLocationId,
                    ),
                ),
            )
        }

        val result = NpcDecisionService(provider).decide(world, aldric.id)

        assertTrue(result is DecisionOutcome.Applied)
        result as DecisionOutcome.Applied
        val updated = result.world.villagers.getValue(aldric.id)
        assertEquals(DecisionSource.AI, result.source)
        assertEquals(1, result.world.revision)
        assertEquals(aldric.currentLocationId, updated.currentLocationId)
        assertEquals(aldric.workplaceLocationId, updated.plannedDestinationId)
        assertEquals(NpcActivity.TRAVELING, updated.activity)
        assertEquals(aldric.id, receivedContext.npcId)
        assertEquals(10, receivedContext.knownLocations.size)
    }

    @Test
    fun `stale AI proposal is rejected without changing the world`() = runBlocking {
        val world = WorldInitializer.createFirstHamlet()
        val villager = world.villagers.values.first()
        val provider = AiDecisionProvider { context ->
            ProviderResult.Success(
                DecisionProposal(
                    npcId = context.npcId,
                    basedOnWorldRevision = context.worldRevision - 1,
                    reasoningSummary = "This decision was made earlier.",
                    action = ProposedAction(ProposedActionType.REST),
                ),
            )
        }

        val result = NpcDecisionService(provider).decide(world, villager.id)

        assertEquals(DecisionOutcome.Rejected(world, "Decision is stale."), result)
    }

    @Test
    fun `invalid destination is rejected without changing the world`() = runBlocking {
        val world = WorldInitializer.createFirstHamlet()
        val villager = world.villagers.values.first()
        val provider = AiDecisionProvider { context ->
            ProviderResult.Success(
                DecisionProposal(
                    npcId = context.npcId,
                    basedOnWorldRevision = context.worldRevision,
                    reasoningSummary = "Go somewhere.",
                    action = ProposedAction(ProposedActionType.TRAVEL_TO, "unknown-place"),
                ),
            )
        }

        val result = NpcDecisionService(provider).decide(world, villager.id)

        assertEquals(DecisionOutcome.Rejected(world, "Travel destination does not exist."), result)
    }

    @Test
    fun `provider outage uses a visible deterministic fallback`() = runBlocking {
        val world = WorldInitializer.createFirstHamlet()
        val aldric = world.villagers.values.single { it.name == "Aldric" }
        val provider = AiDecisionProvider {
            ProviderResult.Failure(ProviderFailure.RATE_LIMITED)
        }

        val result = NpcDecisionService(provider).decide(world, aldric.id)

        assertTrue(result is DecisionOutcome.Applied)
        result as DecisionOutcome.Applied
        assertEquals(DecisionSource.FALLBACK, result.source)
        assertEquals(ProviderFailure.RATE_LIMITED, result.degradedReason)
        assertEquals(aldric.workplaceLocationId, result.world.villagers.getValue(aldric.id).plannedDestinationId)
        assertEquals(aldric.currentLocationId, result.world.villagers.getValue(aldric.id).currentLocationId)
    }

    @Test
    fun `work is rejected until the villager reaches their workplace`() = runBlocking {
        val world = WorldInitializer.createFirstHamlet()
        val aldric = world.villagers.values.single { it.name == "Aldric" }
        val provider = AiDecisionProvider { context ->
            ProviderResult.Success(
                DecisionProposal(
                    npcId = context.npcId,
                    basedOnWorldRevision = context.worldRevision,
                    reasoningSummary = "I will work now.",
                    action = ProposedAction(ProposedActionType.WORK),
                ),
            )
        }

        val result = NpcDecisionService(provider).decide(world, aldric.id)

        assertEquals(DecisionOutcome.Rejected(world, "Villager is not at the workplace."), result)
        assertNull(world.villagers.getValue(aldric.id).plannedDestinationId)
    }
}
