package com.propertylisting.states

import com.sun.javafx.beans.IDProperty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.*

object PolicySchema

object PolicySchema1 : MappedSchema(
        schemaFamily = PolicySchema.javaClass,
        version = 1,
        mappedTypes = listOf(PolicyData::class.java)) {

    @Entity
    @Table(name = "PolicyState")
    class PolicyData(

            @Column(name = "pol_number")
            var polNumber: String,

            @Column(name = "ins_name")
            var insName: String,

            @Column(name = "ins_email")
            var insEmail: String,

            @Column(name = "ins_mobile")
            var insMobile: String,

            @Column(name = "vehicle_number")
            var vehicleNumber: String,

            @Column(name = "pol_premium")
            var polPremium: Int,

            @Column(name = "pol_issue_date")
            var polIssueDate: String,

            @Column(name = "pol_expiry_date")
            var polExpiryDate: String,

            @Column(name = "pol_status")
            var polStatus: String,

            @Column(name = "policy_id")
            var linearId: UUID

    ) : PersistentState() {
        constructor(): this("","","","","",0,"","","", UUID.randomUUID())
    }
}