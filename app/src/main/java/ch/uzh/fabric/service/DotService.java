package ch.uzh.fabric.service;

import ch.uzh.fabric.model.Car;
import ch.uzh.fabric.model.ProposalData;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Service
public class DotService extends TrxService {
    public Collection<ProposalData> getRegistrationProposals(String username, String role) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role});
        request.setFcn("readRegistrationProposalsAsList");

        return query(request, hfc.getChain(), new TypeToken<Collection<ProposalData>>(){}.getType());
    }

    public void register(String owner, String role, String vin) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("register");
        request.setArgs(new String[]{owner, role, vin});
        executeTrx(request, hfc.getChain());
    }

    public Collection<Car> getCarsToConfirm(String username, String role) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setFcn("getCarsToConfirmAsList");
        request.setArgs(new String[]{username, role});

        Collection<Car> result = query(request, hfc.getChain(), new TypeToken<Collection<Car>>(){}.getType());
        if (result == null) {
            result = new ArrayList<Car>();
        }

        return result;
    }

    public void confirm(String username, String role, String vin, String numberplate) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("confirm");
        request.setArgs(new String[]{username, role, vin, numberplate});
        executeTrx(request, hfc.getChain());
    }

    public Collection<Car> getAllCars(String username, String role) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setFcn("getAllCarsAsList");
        request.setArgs(new String[]{username, role});

        Collection<Car> result = query(request, hfc.getChain(), new TypeToken<Collection<Car>>(){}.getType());
        if (result == null) {
            result = new ArrayList<Car>();
        }

        return result;
    }

    public Map<String, String> getRevocationProposals(String username, String role) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setFcn("getRevocationProposals");
        request.setArgs(new String[]{username, role});

        return query(request, hfc.getChain(), new TypeToken<Map<String, String>>(){}.getType());
    }

    public void revoke(String owner, String role, String vin) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("revoke");
        request.setArgs(new String[]{owner, role, vin});
        executeTrx(request, hfc.getChain());
    }
}
