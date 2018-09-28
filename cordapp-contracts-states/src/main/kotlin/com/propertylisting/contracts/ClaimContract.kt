package com.propertylisting.contracts

import com.propertylisting.states.CarState
import com.propertylisting.states.ClaimState
import com.propertylisting.states.PropertyState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class ClaimContract : Contract {
    companion object {
        @JvmStatic
        val CLAIM_CONTRACT_ID = "com.propertylisting.contracts.ClaimContract"
    }

    interface Commands : CommandData {
        class Create : Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[0]
        when (command.value) {
            is Commands.Create -> {
                requireThat {
                    "One input must be consumed" using (tx.inputs.size == 1)
                    "Two outputs should be created" using (tx.outputs.size == 2)
                    val outputState = tx.outputsOfType<ClaimState>().single()
                    "Claim Amount should be greater than zero" using (outputState.claimedAmount > 0)
                }
            }
        }
    }
}