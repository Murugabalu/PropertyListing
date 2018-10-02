package com.propertylisting.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class PolicyState(

        val polNumber: String,
        val insName: Party,
        val insEmail: String,
        val insMobile: String,
        var vehicleNumber: String,
        val polPremium: Int,
        val polIssueDate: String,
        val polExpiryDate: String,
        val polStatus: String,
        val polIssuer: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(insName, polIssuer)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PolicySchema1 -> PolicySchema1.PolicyData(
                    this.polNumber,
                    this.insName.name.toString(),
                    this.insEmail,
                    this.insMobile,
                    this.vehicleNumber,
                    this.polPremium,
                    this.polIssueDate,
                    this.polExpiryDate,
                    this.polStatus,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("The schema : $schema does not exist.")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PolicySchema1)
}
