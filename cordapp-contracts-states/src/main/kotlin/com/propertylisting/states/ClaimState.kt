package com.propertylisting.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class ClaimState(

        val claimNumber: Int,
        val polNumber: Int,
        val insName: Party,
        val claimReason: String,
        val claimedAmount: Int,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(insName)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ClaimSchema1 -> ClaimSchema1.ClaimData(
                    this.claimNumber,
                    this.polNumber,
                    this.insName.name.toString(),
                    this.claimReason,
                    this.claimedAmount,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("The schema : $schema does not exist.")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ClaimSchema1)
}
