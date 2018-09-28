package com.propertylisting.flows

import co.paralleluniverse.fibers.Suspendable
import com.propertylisting.contracts.CarContract
import com.propertylisting.contracts.PolicyContract
import com.propertylisting.flows.RegisterCarFlow
import com.propertylisting.states.CarState
import com.propertylisting.states.PolicyState
import com.propertylisting.states.RecordLostState
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ApplyInsuranceFlow {

    @InitiatingFlow
    @StartableByRPC
    class InsuranceInitiator(val vehicleId : Int) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new IOU.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {

            val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()

            progressTracker.currentStep = GENERATING_TRANSACTION

            val carPage = serviceHub.vaultService.queryBy(CarState::class.java)

            val inpCarRef: StateAndRef<CarState> = carPage.states.stream().filter {
                                                        e -> e.state.data.carNumber == vehicleId
                                                        }.findAny().orElseThrow<IllegalArgumentException?> {
                                                            IllegalArgumentException("No car found for given vehicle Id")
                                                            }

            val inpCar : CarState = inpCarRef.state.data

            var outCar = CarState(inpCar.carNumber, inpCar.owner, inpCar.emailId, inpCar.mobileNo, inpCar.make, inpCar.model, inpCar.price, "Insured", inpCar.linearId)

            val insurer = serviceHub.myInfo.legalIdentities.first()
            val issuer = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse("O=PartyB,L=New York,C=US"))

            val polPremium = inpCar.price * 0.2

            val current = LocalDateTime.now()
            val nextYear = current.plusYears(1)
            val formatter = DateTimeFormatter.ISO_DATE

            val issueDate = current.format(formatter)
            val expiry = nextYear.format(formatter)

            val polStatus = "Un Claimed"

            val policyState = PolicyState (
                    polNumber = 1,
                    insName = insurer,
                    insEmail = outCar.emailId,
                    insMobile = outCar.mobileNo,
                    vehicleId = outCar.carNumber,
                    polPremium = polPremium.toInt(),
                    polIssueDate = issueDate,
                    polExpiryDate = expiry,
                    polStatus = polStatus,
                    polIssuer = issuer!!)

            println("Input and output created")

            val txCommand1 = Command(CarContract.Commands.Consume(), listOf(insurer.owningKey, issuer!!.owningKey))
            val txCommand2 = Command(PolicyContract.Commands.Create(), listOf(insurer.owningKey, issuer!!.owningKey))

            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inpCarRef)
                    .addOutputState(outCar, CarContract.CAR_CONTRACT_ID)
                    .addOutputState(policyState, PolicyContract.POLICY_CONTRACT_ID)
                    .addCommand(txCommand1)
                    .addCommand(txCommand2)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION
            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION
            // Sign the transaction.
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            // Stage 4.
            progressTracker.currentStep = GATHERING_SIGS
            // Send the state to the counterparty, and receive it back with their signature.
            val issuerFlow = initiateFlow(issuer)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(issuerFlow), GATHERING_SIGS.childProgressTracker()))

            // Stage 5.
            progressTracker.currentStep = FINALISING_TRANSACTION
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(InsuranceInitiator::class)
    class AcceptInsurance(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                    println("Counter party flow here")

                    val carOutput = stx.tx.getOutput(0)
                    "The output 1 must be a car State." using (carOutput is CarState)

                    println("Car Output determined")

                    val carObj = carOutput as CarState
                    println("Car Object determined" + carObj.carNumber)

                    val recLostPage = serviceHub.vaultService.queryBy(RecordLostState::class.java)
                    println("Rec lost paged")

                    val recLostRef: Boolean = recLostPage.states.stream().filter {
                                        e -> e.state.data.carId == carObj.carNumber
                                        }.findAny().isPresent
                    println(recLostRef)

                    "Car is already lost, can not be insured." using (!recLostRef)
                    println("Car Not found So continues")

                    val polOutput = stx.tx.getOutput(1)
                    "The output 2 must be a policy State." using (polOutput is PolicyState)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}