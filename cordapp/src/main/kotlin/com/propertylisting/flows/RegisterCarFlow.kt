package com.propertylisting.flows

import co.paralleluniverse.fibers.Suspendable
import com.propertylisting.contracts.CarContract
import com.propertylisting.contracts.CarContract.Companion.CAR_CONTRACT_ID
import com.propertylisting.states.CarState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.ProgressTracker.Step
import net.corda.core.flows.FinalityFlow

@StartableByRPC
class RegisterCarFlow(
        val carNumber : String,
        val emailId : String,
        val mobileNumber : String,
        val make : String,
        val model : String,
        val price : Long) : FlowLogic<SignedTransaction?>() {

    companion object {

        object GENERATING_REGISTRATION_TRANSACTION : Step("Generating transaction for Property Registration.")
        object VERIFYING_REGISTRATION_TRANSACTION : Step("Verifying contract constraints.")
        object SIGNING_REGISTRATION_TRANSACTION : Step("Signing transaction with propertylisting owner's private key.")
        object FINALISING_REGISTRATION_TRANSACTION : Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_REGISTRATION_TRANSACTION,
                VERIFYING_REGISTRATION_TRANSACTION,
                SIGNING_REGISTRATION_TRANSACTION,
                FINALISING_REGISTRATION_TRANSACTION
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction?{

        val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

        progressTracker.currentStep = GENERATING_REGISTRATION_TRANSACTION

        //To check if the car is already registered for he Initiator
        FlowHelper.checkIfCarExists(carNumber, serviceHub)

        val carState = CarState(
                carNumber = carNumber,
                owner = serviceHub.myInfo.legalIdentities.first(),
                emailId = emailId,
                mobileNo = mobileNumber,
                make = make,
                model = model,
                price = price,
                insStatus = "Not Insured")

        val txCommand = Command(CarContract.Commands.Create(), carState.participants.map { it.owningKey })

        val txBuilder = TransactionBuilder(notary)
                .addOutputState(carState, CAR_CONTRACT_ID)
                .addCommand(txCommand)

        progressTracker.currentStep = VERIFYING_REGISTRATION_TRANSACTION
        txBuilder.verify(serviceHub)

        progressTracker.currentStep = SIGNING_REGISTRATION_TRANSACTION
        val signedTx = serviceHub.signInitialTransaction(txBuilder)

        progressTracker.currentStep = FINALISING_REGISTRATION_TRANSACTION
        return subFlow(FinalityFlow(signedTx))

    }
}