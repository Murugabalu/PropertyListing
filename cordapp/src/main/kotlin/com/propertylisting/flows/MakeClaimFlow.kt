package com.propertylisting.flows

import co.paralleluniverse.fibers.Suspendable
import com.propertylisting.contracts.ClaimContract
import com.propertylisting.contracts.PolicyContract
import com.propertylisting.flows.FlowHelper.getRandomClaimNumber
import com.propertylisting.states.ClaimState
import com.propertylisting.states.PolicyState
import com.propertylisting.states.RecordLostState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

object MakeClaimFlow {

    @InitiatingFlow
    @StartableByRPC
    class ClaimInitiator(val policyNumber : String,
                    val claimReason : String) : FlowLogic<SignedTransaction>() {

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

            println("Inside Call")

            val notary: Party = serviceHub.networkMapCache.notaryIdentities.first()
            println("party:$notary")

            progressTracker.currentStep = GENERATING_TRANSACTION
            println("Step 1 done")

            val policyPage = serviceHub.vaultService.queryBy(PolicyState::class.java)
            println("Policy paged")

            val inpPolRef: StateAndRef<PolicyState> = policyPage.states.stream().filter {
                                e -> e.state.data.polNumber == policyNumber
                            }.findAny().orElseThrow<IllegalArgumentException?> {
                                IllegalArgumentException("No policy found under given Policy Number$policyNumber")
                            }
            println("Policy found")

            val inpPolicy : PolicyState = inpPolRef.state.data
            println("Policy State input")

            var outPolicy = PolicyState(polNumber = inpPolicy.polNumber,
                                        insName = inpPolicy.insName,
                                        insEmail =  inpPolicy.insEmail,
                                        insMobile = inpPolicy.insMobile,
                                        vehicleNumber = inpPolicy.vehicleNumber,
                                        polPremium = inpPolicy.polPremium,
                                        polIssueDate = inpPolicy.polIssueDate,
                                        polExpiryDate = inpPolicy.polExpiryDate,
                                        polStatus = "Policy Claimed",
                                        polIssuer = inpPolicy.polIssuer,
                                        linearId = inpPolicy.linearId)

            println("Policy state output" + inpPolicy.polStatus + "Output Status" + outPolicy.polStatus)

            val claimAmount : Int = if (claimReason=="Lost") outPolicy.polPremium else (outPolicy.polPremium * 0.5).toInt()

            val insurer = serviceHub.myInfo.legalIdentities.first()
            val issuer = serviceHub.identityService.wellKnownPartyFromX500Name(CordaX500Name.parse("O=PartyB,L=New York,C=US"))

            val claimState = ClaimState (
                    claimNumber = getRandomClaimNumber(),
                    polNumber = policyNumber,
                    insName = insurer,
                    claimReason = claimReason,
                    claimedAmount = claimAmount)

            println("Input and output created")

            val txCommand1 = Command(PolicyContract.Commands.Consume(), listOf(insurer.owningKey, issuer!!.owningKey))
            val txCommand2 = Command(ClaimContract.Commands.Create(), listOf(insurer.owningKey, issuer!!.owningKey))

            val txBuilder = TransactionBuilder(notary)
                    .addInputState(inpPolRef)
                    .addOutputState(claimState, ClaimContract.CLAIM_CONTRACT_ID)
                    .addOutputState(outPolicy, PolicyContract.POLICY_CONTRACT_ID)
                    .addCommand(txCommand2)
                    .addCommand(txCommand1)

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

    @InitiatedBy(ClaimInitiator::class)
    class AcceptClaim(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {

                    println("Counter party flow here")

                    val policyOutput = stx.tx.getOutput(1)
                    "The output 1 must be a policy State." using (policyOutput is PolicyState)

                    println("Car Output determined")

                    val polObj = policyOutput as PolicyState
                    println("Policy Number determined" + polObj.polNumber)

                    val recLostPage = serviceHub.vaultService.queryBy(RecordLostState::class.java)
                    println("Rec lost paged")

                    val recLostRef: Boolean = recLostPage.states.stream().filter {
                                                e -> e.state.data.carNumber == polObj.vehicleNumber
                                                }.findAny().isPresent
                    println(recLostRef)

                    "Car is not yet registered missing or not found." using (recLostRef)
                    println("Car found missing continues")

                    val claimOutput = stx.tx.getOutput(0)
                    "The output 2 must be a policy State." using (claimOutput is ClaimState)

                    println("Signing Here Acceptor")
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}