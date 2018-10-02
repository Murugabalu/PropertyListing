package com.propertylisting.flows

import com.propertylisting.flows.utils.AlreadyRegisteredException
import com.propertylisting.states.CarState
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import io.netty.util.internal.shaded.org.jctools.util.JvmInfo.PAGE_SIZE
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import java.util.concurrent.ThreadLocalRandom

object FlowHelper {

    fun checkIfCarExists(carNumber: String, serviceHub: ServiceHub) {
        var pageSpecification: PageSpecification
        var results: Vault.Page<CarState>
        var pageNum: Int? = DEFAULT_PAGE_NUM
        do {
            pageSpecification = PageSpecification(pageNum!!, PAGE_SIZE)
            results = serviceHub.vaultService.queryBy(CarState::class.java,
                    QueryCriteria.VaultQueryCriteria(
                            Vault.StateStatus.ALL,
                            setOf(CarState::class.java)),
                    pageSpecification)
            if (results.states.stream().anyMatch { (state) -> state.data.carNumber.equals(carNumber) }) {
                throw AlreadyRegisteredException(String.format("Cake already issued with cakeId: %s", carNumber))
            }
            pageNum++
        } while (pageSpecification.pageSize * pageNum!! <= results.totalStatesAvailable)
    }

    fun getRandomPolicyNumber() : String {
        println("Inside generator Method")
        val ALPHANUMS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val policyNumber = StringBuilder()
        while (policyNumber.length < 10) { // length of the random string.
            val index = ThreadLocalRandom.current().nextInt(1, ALPHANUMS.length)
            policyNumber.append(ALPHANUMS[index])
        }
        println("PolicyNumber found:" + policyNumber.toString())
        return policyNumber.toString()
    }

    fun getRandomClaimNumber() : String {
        val ALPHANUMS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
        val claimNumber = StringBuilder()
        while (claimNumber.length < 12) { // length of the random string.
            val index = ThreadLocalRandom.current().nextInt(1, ALPHANUMS.length)
            claimNumber.append(ALPHANUMS[index])
        }
        return claimNumber.toString()
    }
}
