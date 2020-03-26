package net.corda.core.flows

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateRef
import net.corda.core.crypto.SecureHash
import net.corda.core.utilities.unwrap

class NotaryQueryClientFlow {

    @StartableByRPC
    class DoubleSpendAudit(private val stateRef: String) :
        FlowLogic<NotaryQuery.Result.SpentStates>() {

        @Suspendable
        override fun call(): NotaryQuery.Result.SpentStates {
            val (txHash, index) = stateRef.split(':', limit = 2)
            val stateRefObj = StateRef(SecureHash.parse(txHash), index.toInt())

            return subFlow(InitiateQuery(NotaryQuery.Request.SpentStates(stateRefObj)))
                    as NotaryQuery.Result.SpentStates
        }
    }

    /**
     * Trivial flow to send a generic notary query request, and to return the response.
     *
     * Note that it is expected that the receiving flow is only run on the Notary (i.e.
     * both the initiating flow and this are running on the same system), so we do no
     * checking of the identity of the responder or the integrity of the message. This will
     * need to be revisited if this flow is initiated by a Corda node in future.
     */
    @InitiatingFlow
    class InitiateQuery(private val request: NotaryQuery.Request) :
            FlowLogic<NotaryQuery.Result>() {

        @Suspendable
        override fun call() : NotaryQuery.Result {
            val notarySession = initiateFlow(serviceHub.myInfo.legalIdentities.first())
            notarySession.send(request)
            val result = notarySession.receive<NotaryQuery.Result>()
            return result.unwrap { data -> data }
        }
    }
}
