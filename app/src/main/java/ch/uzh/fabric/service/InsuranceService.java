package ch.uzh.fabric.service;

import ch.uzh.fabric.model.Insurer;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.springframework.stereotype.Service;

@Service
public class InsuranceService extends TrxService {
    public void acceptInsurance(String username, String role, String userToInsure, String vin, String company) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("insuranceAccept");
        request.setArgs(new String[]{username, role, userToInsure, vin, company});
        executeTrx(request, hfc.getChain());
    }

    public Insurer getInsurer(String username, String role, String company) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role, company});
        request.setFcn("getInsurer");

        return query(request, hfc.getChain(), new TypeToken<Insurer>(){}.getType());
    }
}
