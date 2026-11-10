package com.hamlet.core

data class KnownLocation(
    val id: String,
    val name: String,
)

data class NpcDecisionContext(
    val npcId: String,
    val worldRevision: Long,
    val name: String,
    val occupation: String,
    val personalityTraits: Set<String>,
    val ambition: String,
    val currentLocationId: String,
    val workplaceLocationId: String?,
    val currentActivity: NpcActivity,
    val knownLocations: List<KnownLocation>,
)

enum class ProposedActionType {
    TRAVEL_TO,
    WORK,
    REST,
    OBSERVE,
    END_ACTIVITY,
}

data class ProposedAction(
    val type: ProposedActionType,
    val targetLocationId: String? = null,
)

data class DecisionProposal(
    val npcId: String,
    val basedOnWorldRevision: Long,
    val reasoningSummary: String,
    val action: ProposedAction,
)

enum class ProviderFailure {
    UNAVAILABLE,
    RATE_LIMITED,
    AUTHENTICATION,
    INVALID_RESPONSE,
}

sealed interface ProviderResult {
    data class Success(val proposal: DecisionProposal) : ProviderResult
    data class Failure(val reason: ProviderFailure) : ProviderResult
}

fun interface AiDecisionProvider {
    suspend fun propose(context: NpcDecisionContext): ProviderResult
}

enum class DecisionSource {
    AI,
    FALLBACK,
}

sealed interface DecisionOutcome {
    data class Applied(
        val world: HamletWorld,
        val action: ProposedAction,
        val source: DecisionSource,
        val degradedReason: ProviderFailure? = null,
    ) : DecisionOutcome

    data class Rejected(
        val world: HamletWorld,
        val reason: String,
    ) : DecisionOutcome
}

class NpcDecisionService(
    private val provider: AiDecisionProvider,
) {
    suspend fun decide(world: HamletWorld, npcId: String): DecisionOutcome {
        val villager = world.villagers[npcId]
            ?: return DecisionOutcome.Rejected(world, "Unknown villager.")
        val context = world.toDecisionContext(villager)

        return when (val result = provider.propose(context)) {
            is ProviderResult.Success -> apply(world, context, result.proposal, DecisionSource.AI)
            is ProviderResult.Failure -> {
                val fallback = fallbackProposal(context)
                apply(world, context, fallback, DecisionSource.FALLBACK, result.reason)
            }
        }
    }

    private fun apply(
        world: HamletWorld,
        context: NpcDecisionContext,
        proposal: DecisionProposal,
        source: DecisionSource,
        degradedReason: ProviderFailure? = null,
    ): DecisionOutcome {
        val rejection = validateProposal(world, context, proposal)
        if (rejection != null) return DecisionOutcome.Rejected(world, rejection)

        val villager = world.villagers.getValue(context.npcId)
        val updatedVillager = when (proposal.action.type) {
            ProposedActionType.TRAVEL_TO -> villager.copy(
                activity = NpcActivity.TRAVELING,
                plannedDestinationId = proposal.action.targetLocationId,
            )
            ProposedActionType.WORK -> villager.copy(
                activity = NpcActivity.WORKING,
                plannedDestinationId = null,
            )
            ProposedActionType.REST -> villager.copy(
                activity = NpcActivity.RESTING,
                plannedDestinationId = null,
            )
            ProposedActionType.OBSERVE -> villager.copy(
                activity = NpcActivity.OBSERVING,
                plannedDestinationId = null,
            )
            ProposedActionType.END_ACTIVITY -> villager.copy(
                activity = NpcActivity.IDLE,
                plannedDestinationId = null,
            )
        }

        return DecisionOutcome.Applied(
            world = world.copy(
                revision = world.revision + 1,
                villagers = world.villagers + (villager.id to updatedVillager),
            ),
            action = proposal.action,
            source = source,
            degradedReason = degradedReason,
        )
    }

    private fun validateProposal(
        world: HamletWorld,
        context: NpcDecisionContext,
        proposal: DecisionProposal,
    ): String? {
        if (proposal.npcId != context.npcId) return "Decision belongs to a different villager."
        if (proposal.basedOnWorldRevision != world.revision) return "Decision is stale."
        if (proposal.reasoningSummary.isBlank() || proposal.reasoningSummary.length > MAX_REASONING_LENGTH) {
            return "Decision summary is missing or too long."
        }

        return when (proposal.action.type) {
            ProposedActionType.TRAVEL_TO -> {
                val destinationId = proposal.action.targetLocationId
                    ?: return "Travel decision has no destination."
                when {
                    destinationId !in world.locations -> "Travel destination does not exist."
                    destinationId == context.currentLocationId -> "Villager is already at the destination."
                    else -> null
                }
            }
            ProposedActionType.WORK -> when {
                proposal.action.targetLocationId != null -> "Work decision has unexpected parameters."
                context.workplaceLocationId == null -> "Villager has no workplace."
                context.currentLocationId != context.workplaceLocationId -> "Villager is not at the workplace."
                else -> null
            }
            ProposedActionType.REST,
            ProposedActionType.OBSERVE,
            ProposedActionType.END_ACTIVITY,
            -> if (proposal.action.targetLocationId != null) {
                "Action has unexpected parameters."
            } else {
                null
            }
        }
    }

    private fun fallbackProposal(context: NpcDecisionContext): DecisionProposal {
        val workplaceId = context.workplaceLocationId
        val action = when {
            workplaceId != null && workplaceId != context.currentLocationId ->
                ProposedAction(ProposedActionType.TRAVEL_TO, workplaceId)
            workplaceId != null -> ProposedAction(ProposedActionType.WORK)
            else -> ProposedAction(ProposedActionType.REST)
        }
        return DecisionProposal(
            npcId = context.npcId,
            basedOnWorldRevision = context.worldRevision,
            reasoningSummary = "Use a routine local activity while AI is unavailable.",
            action = action,
        )
    }

    private companion object {
        const val MAX_REASONING_LENGTH = 240
    }
}

private fun HamletWorld.toDecisionContext(villager: Villager) = NpcDecisionContext(
    npcId = villager.id,
    worldRevision = revision,
    name = villager.name,
    occupation = villager.occupation,
    personalityTraits = villager.personalityTraits,
    ambition = villager.ambition,
    currentLocationId = villager.currentLocationId,
    workplaceLocationId = villager.workplaceLocationId,
    currentActivity = villager.activity,
    knownLocations = locations.values.map { KnownLocation(it.id, it.name) },
)
