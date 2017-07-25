package ch.uzh.fabric.service;

import ch.uzh.fabric.controller.AppController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public abstract class HFCService {
    private Gson g = new GsonBuilder().create();

    private ChainCodeID chainCodeID = ChainCodeID.newBuilder()
            .setName(AppController.CHAIN_CODE_NAME)
            .setVersion(AppController.CHAIN_CODE_VERSION)
            .setPath(AppController.CHAIN_CODE_PATH).build();

    protected String getCCErrorMsg(Collection<ProposalResponse> failedProposals) {
        String error = failedProposals.iterator().next().getMessage();
        return error.substring(error.indexOf("message: ") + 9, error.indexOf("), cause"));
    }

    protected Collection<ProposalResponse> executeTrx(TransactionProposalRequest request, Chain chain) throws Exception {
        request.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(request, chain.getPeers());
        for (ProposalResponse response : invokePropResp) {
            if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        if (!failed.isEmpty()) throw new ProposalException(getCCErrorMsg(failed));

        chain.sendTransaction(successful).get(AppController.TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
        return successful;
    }
}
