package com.propertylisting.states

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object ClaimSchema

object ClaimSchema1 : MappedSchema(
        schemaFamily = ClaimSchema.javaClass,
        version = 1,
        mappedTypes = listOf(ClaimData::class.java)) {

    @Entity
    @Table(name = "ClaimState")
    class ClaimData(

            @Column(name="claim_number")
            var claimNum: Int,

            @Column(name = "pol_number")
            var polNumber: Int,

            @Column(name = "ins_name")
            var insName: String,

            @Column(name = "claim_reason")
            var claimReason: String,

            @Column(name = "claim_amount")
            var claimAmount: Int,

            @Column(name = "claim_id")
            var linearId: UUID

    ) : PersistentState() {
        constructor(): this(0,0,"","",0, UUID.randomUUID())
    }
}