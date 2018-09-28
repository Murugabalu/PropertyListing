package com.propertylisting.flows.utils

import com.propertylisting.states.CarState
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import io.netty.util.internal.shaded.org.jctools.util.JvmInfo.PAGE_SIZE
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM

object FlowHelper {

    fun checkIfCarExists(carNumber: Int, serviceHub: ServiceHub) {
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
}
