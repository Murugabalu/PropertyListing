package com.propertylisting.contracts

import com.propertylisting.states.RecordLostState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class RecordLostContract : Contract {
    companion object {
        @JvmStatic
        val RECORD_LOST_CONTRACT_ID = "com.propertylisting.contracts.RecordLostContract"
    }

    interface Commands : CommandData {
        class Create : Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> {
                requireThat {
                    "No inputs should be consumed for recording Lost car" using (tx.inputs.isEmpty())
                    "Only one output state should be created for Lost Car" using (tx.outputs.size == 1)

                    val outputState = tx.outputsOfType<RecordLostState>().single()
                    "The transaction should have only one participant" using (outputState.participants.size == 1)
                    "Car should either be lost or damaged" using (outputState.recReason =="Lost" || outputState.recReason == "Damaged")
                }
            }
        }
    }
}