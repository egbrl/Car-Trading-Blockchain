package ch.uzh.fabric.service;

import ch.uzh.fabric.controller.AppController;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

public abstract class HFCService {
    protected Gson g = new GsonBuilder().create();

    private ChainCodeID chainCodeID = ChainCodeID.newBuilder()
            .setName(AppController.CHAIN_CODE_NAME)
            .setVersion(AppController.CHAIN_CODE_VERSION)
            .setPath(AppController.CHAIN_CODE_PATH).build();

    protected String getCCErrorMsg(Collection<ProposalResponse> failedProposals) {
        String error = failedProposals.iterator().next().getMessage();
        return error.substring(error.indexOf("message: ") + 9, error.indexOf("), cause"));
    }

    protected <T> T query(QueryByChaincodeRequest request, Chain chain, Type type) throws Exception {
        request.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = chain.queryByChaincode(request);
        } catch (InvalidArgumentException | ProposalException e) {
            throw new CompletionException(e);
        }

        T result = null;
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                throw new Exception(proposalResponse.getMessage());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                result = g.fromJson(payload, type);
            }
        }

        return result;
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
