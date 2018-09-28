package com.propertylisting.states

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object RecordLostSchema

object RecordLostSchema1 : MappedSchema(
        schemaFamily = RecordLostSchema.javaClass,
        version = 1,
        mappedTypes = listOf(RecordLostData::class.java)) {

    @Entity
    @Table(name = "RecordLostState")
    class RecordLostData(

            @Column(name="record_number")
            var recNumber: Int,

            @Column(name = "cust_name")
            var custName: String,

            @Column(name = "email")
            var email: String,

            @Column(name = "mobile")
            var mobile: String,

            @Column(name = "vehicle_id")
            var vehicleId: Int,

            @Column(name = "record_reason")
            var recReason: String,

            @Column(name = "record_id")
            var linearId: UUID

    ) : PersistentState() {
        constructor(): this(0,"","","",0,"", UUID.randomUUID())
    }
}