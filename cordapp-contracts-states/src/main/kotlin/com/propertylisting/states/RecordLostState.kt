package com.propertylisting.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class RecordLostState(

        val recNumber: Int,
        val custName: Party,
        val emailId: String,
        val mobileNo: String,
        val carNumber: String,
        val recReason: String,
        val recParty: Party,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(recParty)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is RecordLostSchema1 -> RecordLostSchema1.RecordLostData(
                    this.recNumber,
                    this.custName.name.toString(),
                    this.emailId,
                    this.mobileNo,
                    this.carNumber,
                    this.recReason,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("The schema : $schema does not exist.")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RecordLostSchema1)
}
