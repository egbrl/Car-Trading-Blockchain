package ch.uzh.fabric.service;

import ch.uzh.fabric.config.TestConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

abstract class TrxService {
    @Autowired
    HfcService hfc;

    private static final TestConfig testConfig = TestConfig.getConfig();
    static final Gson g = new GsonBuilder().create();

    String getCCErrorMsg(Collection<ProposalResponse> failedProposals) {
        String error = failedProposals.iterator().next().getMessage();
        return error.substring(error.indexOf("message: ") + 9, error.indexOf("), cause"));
    }

    <T> T query(QueryByChaincodeRequest request, Chain chain, Type type) throws Exception {
        request.setChaincodeID(hfc.getCcId());

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

    Collection<ProposalResponse> executeTrx(TransactionProposalRequest request, Chain chain) throws Exception {
        request.setChaincodeID(hfc.getCcId());

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

        chain.sendTransaction(successful).get(testConfig.getTransactionWaitTime(), TimeUnit.SECONDS);
        return successful;
    }
}
