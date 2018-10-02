package com.propertylisting.states

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CarSchema

object CarSchema1 : MappedSchema(
        schemaFamily = CarSchema.javaClass,
        version = 1,
        mappedTypes = listOf(CarData::class.java)) {

    @Entity
    @Table(name = "CarState")
    class CarData(

            @Column(name="car_number")
            var carNumber: String,

            @Column(name = "owner")
            var owner: String,

            @Column(name = "email")
            var email: String,

            @Column(name = "mobile")
            var mobile: String,

            @Column(name = "make")
            var make: String,

            @Column(name = "model")
            var model: String,

            @Column(name = "price")
            var price: Long,

            @Column(name = "insStatus")
            var insStatus: String,

            @Column(name = "car_id")
            var linearId: UUID

    ) : PersistentState() {
        constructor(): this("","","","","","",0L,"", UUID.randomUUID())
    }
}