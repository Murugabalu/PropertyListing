package com.propertylisting.states

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

data class CarState(

        val carNumber: Int,
        val owner: Party,
        val emailId: String,
        val mobileNo: String,
        val make: String,
        val model: String,
        val price: Long,
        val insStatus: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, QueryableState {

    override val participants: List<AbstractParty> get() = listOf(owner)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is CarSchema1 -> CarSchema1.CarData(
                    this.carNumber,
                    this.owner.name.toString(),
                    this.emailId,
                    this.mobileNo,
                    this.make,
                    this.model,
                    this.price,
                    this.insStatus,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("The schema : $schema does not exist.")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(CarSchema1)
}
