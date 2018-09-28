package com.propertylisting.contracts

import com.propertylisting.states.CarState
import com.propertylisting.states.PropertyState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class CarContract : Contract {
    companion object {
        @JvmStatic
        val CAR_CONTRACT_ID = "com.propertylisting.contracts.CarContract"
    }

    interface Commands : CommandData {
        class Create : Commands
        class Consume : Commands
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands[0]

        when (command.value) {
            is Commands.Create -> {
                requireThat {
                    "No inputs should be consumed for car registration" using (tx.inputs.isEmpty())
                    "Only one output state should be created after car registration" using (tx.outputs.size == 1)
                    val outputState = tx.outputsOfType<CarState>().single()
                    "The transaction should have only one participant" using (outputState.participants.size == 1)
                    "The transaction must be signed by the car owner" using (outputState.owner.owningKey == command.signers.single())
                    "Car model is a mandatory field" using (!outputState.model.isEmpty())
                   // "Car variant is a mandatory field" using (!outputState.variant.isEmpty())
                    "Car price should be greater than zero" using (outputState.price > 0)
                }
            }

            is Commands.Consume -> {

                val inputState = tx.inputsOfType<CarState>()
                val outputState = tx.outputsOfType<CarState>()

                requireThat {
                    "One Input should be consumed" using (tx.inputs.size == 1)
                    "Two outputs should be produced" using (tx.outputs.size == 2)
                }
            }
        }
    }
}