package com.propertylisting.contracts

import com.propertylisting.states.CarState
import com.propertylisting.states.PolicyState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction

class PolicyContract : Contract {
    companion object {
        @JvmStatic
        val POLICY_CONTRACT_ID = "com.propertylisting.contracts.PolicyContract"
    }

    interface Commands : CommandData {
        class Create : Commands
        class Consume : Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands[1]

        when (command.value) {
            is Commands.Create -> {
                requireThat {
                    "Policy creation should have one Input" using (tx.inputs.size == 1)
                    "There should be two outputs in Policy Creation" using (tx.outputs.size == 2)
                    val outputState = tx.outputsOfType<PolicyState>().single()
                    "The transaction must be signed by the Insurer" using (outputState.polPremium > 0)

                }
            }
            is Commands.Consume -> {
                requireThat {
                    "Claim creating should have one Input" using (tx.inputs.size == 1)
                    "There should be two outputs in Claim Creation" using (tx.outputs.size == 2)
                }
            }
        }
    }
}