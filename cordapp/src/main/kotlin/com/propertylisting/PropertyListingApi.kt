package com.propertylisting

import com.propertylisting.flows.ApplyInsuranceFlow.InsuranceInitiator
import com.propertylisting.flows.MakeClaimFlow.ClaimInitiator
import com.propertylisting.flows.PropertyRegistrationFlow
import com.propertylisting.flows.RecordLostVehicleFlow
import com.propertylisting.flows.RegisterCarFlow
import com.propertylisting.flows.RequestPropertyListFlow.Initiator
import com.propertylisting.states.CarState
import com.propertylisting.states.ClaimState
import com.propertylisting.states.PolicyState
import com.propertylisting.states.PropertyState
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.utilities.getOrThrow
import org.eclipse.jetty.http.HttpStatus
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

val SERVICE_NAMES = listOf("Notary", "Network Map Service")

@Path("propertyListing")
class PropertyListingApi(private val rpcOps: CordaRPCOps) {
    private val myLegalName: CordaX500Name = rpcOps.nodeInfo().legalIdentities.first().name

    @GET
    @Path("myName")
    @Produces(MediaType.APPLICATION_JSON)
    fun myName() = mapOf("myName" to myLegalName)

    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    fun peers() = mapOf("peers" to rpcOps.networkMapSnapshot()
            .map { it.legalIdentities.first().name }
            .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })

    @GET
    @Path("registeredVehicles")
    @Produces(MediaType.APPLICATION_JSON)
    fun registeredVehiclesList() = rpcOps.vaultQuery(CarState::class.java).states
            .filter { (state) -> state.data.owner.name == myLegalName }

    @GET
    @Path("insuredVehicles")
    @Produces(MediaType.APPLICATION_JSON)
    fun insuredVehiclesList() = rpcOps.vaultQuery(CarState::class.java).states
            .filter { (state) -> state.data.owner.name == myLegalName && state.data.insStatus == "Insured"}

    @GET
    @Path("policies")
    @Produces(MediaType.APPLICATION_JSON)
    fun policyList() = rpcOps.vaultQuery(PolicyState::class.java).states
            .filter { (state) -> state.data.insName.name == myLegalName }

    @GET
    @Path("claims")
    @Produces(MediaType.APPLICATION_JSON)
    fun claimList() = rpcOps.vaultQuery(ClaimState::class.java).states
            .filter { (state) -> state.data.insName.name == myLegalName }

    @POST
    @Path("registerCar")
    fun carRegistration(@QueryParam("carNumber") carNumber: Int,
                         @QueryParam("emailId") emailId: String,
                         @QueryParam("mobileNumber") mobileNumber: String,
                         @QueryParam("make") make: String,
                         @QueryParam("model") model: String,
                         @QueryParam("price") price: Long): Response
    {
        return try {
            val signedTx = rpcOps.startTrackedFlow(::RegisterCarFlow, carNumber,emailId,mobileNumber, make, model, price).returnValue.getOrThrow()
            if(signedTx == null)
                Response.status(HttpStatus.CONFLICT_409).entity("Property with this address is already registered.").build()
            else
                Response.status(HttpStatus.CREATED_201).entity("Property is registered successfully.").build()
        } catch (ex: Throwable) {
            Response.status(HttpStatus.BAD_REQUEST_400).entity("Error in Property Registration : " + ex.message!!).build()
        }
    }

    @POST
    @Path("applyInsurance")
    fun applyInsurance(@QueryParam("vehicleId") vehicleId: Int): Response
    {
        return try {
            val signedTx = rpcOps.startTrackedFlow(::InsuranceInitiator, vehicleId).returnValue.getOrThrow()
            if(signedTx == null)
                Response.status(HttpStatus.CONFLICT_409).entity("Property with this address is already registered.").build()
            else
                Response.status(HttpStatus.CREATED_201).entity("Property is registered successfully.").build()
        } catch (ex: Throwable) {
            Response.status(HttpStatus.BAD_REQUEST_400).entity("Error in Property Registration : " + ex.message!!).build()
        }
    }

    @POST
    @Path("makeClaim")
    fun makeClaim(@QueryParam("policyNumber") policyNumber: Int,
                  @QueryParam("claimReason") claimReason: String): Response
    {
        return try {
            val signedTx = rpcOps.startTrackedFlow(::ClaimInitiator, policyNumber, claimReason).returnValue.getOrThrow()
            if(signedTx == null)
                Response.status(HttpStatus.CONFLICT_409).entity("Property with this address is already registered.").build()
            else
                Response.status(HttpStatus.CREATED_201).entity("Property is registered successfully.").build()
        } catch (ex: Throwable) {
            Response.status(HttpStatus.BAD_REQUEST_400).entity("Error in Property Registration : " + ex.message!!).build()
        }
    }

    @POST
    @Path("recordLostVehicle")
    fun recordLostVehicle(@QueryParam("recNumber") recNumber: Int,
                        @QueryParam("custName") custName: CordaX500Name,
                        @QueryParam("emailId") emailId: String,
                        @QueryParam("mobileNumber") mobileNumber: String,
                        @QueryParam("vehicleId") vehicleId: Int,
                        @QueryParam("recReason") recReason: String): Response
    {
        return try {
            val custParty : Party = rpcOps.wellKnownPartyFromX500Name(custName)!!
            val signedTx = rpcOps.startTrackedFlow(::RecordLostVehicleFlow, recNumber, custParty,emailId,mobileNumber, vehicleId, recReason).returnValue.getOrThrow()
            if(signedTx == null)
                Response.status(HttpStatus.CONFLICT_409).entity("Property with this address is already registered.").build()
            else
                Response.status(HttpStatus.CREATED_201).entity("Property is registered successfully.").build()
        } catch (ex: Throwable) {
            Response.status(HttpStatus.BAD_REQUEST_400).entity("Error in Property Registration : " + ex.message!!).build()
        }
    }

    @POST
    @Path("registration2")
    fun propertyRegistration(@QueryParam("propertyAddress") propertyAddress: String,
                             @QueryParam("propertyArea") propertyArea: Long,
                             @QueryParam("propertySellingPrice") propertySellingPrice: Long): Response
    {
        if(propertyAddress.isEmpty())
            return Response.status(HttpStatus.BAD_REQUEST_400).entity("Property Address is a mandatory field.").build()
        if(propertyArea <= 0)
            return Response.status(HttpStatus.BAD_REQUEST_400).entity("Property Area should be greater than zero.").build()
        if(propertySellingPrice <= 0)
            return Response.status(HttpStatus.BAD_REQUEST_400).entity("Property Selling Price should be greater than zero.").build()
        return try {
            val signedTx = rpcOps.startTrackedFlow(::PropertyRegistrationFlow, propertyAddress,propertyArea,propertySellingPrice).returnValue.getOrThrow()
            if(signedTx == null)
                Response.status(HttpStatus.CONFLICT_409).entity("Property with this address is already registered.").build()
            else
                Response.status(HttpStatus.CREATED_201).entity("Property is registered successfully.").build()
        } catch (ex: Throwable) {
            Response.status(HttpStatus.BAD_REQUEST_400).entity("Error in Property Registration : " + ex.message!!).build()
        }
    }
}