package ch.uzh.fabric.service;

import ch.uzh.fabric.model.*;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Service
public class CarService extends TrxService {

    public Collection<Car> getCars(String username, String role) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role});
        request.setFcn("readUser");

        ArrayList<Car> cars = new ArrayList<>();
        User user = query(request, hfc.getChain(), new TypeToken<User>(){}.getType());
        for (String vin : user.getCars()) {
            Car car = getCar(username, role, vin);
            cars.add(car);
        }

        return cars;
    }

    public Collection<Offer> getSalesOffers(String username, String role) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role});
        request.setFcn("readUser");

        User user = query(request, hfc.getChain(), new TypeToken<User>(){}.getType());
        return user.getOffers();
    }

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

    public Car getCar(String username, String role, String vin) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role, vin});
        request.setFcn("readCar");

        return query(request, hfc.getChain(), new TypeToken<Car>(){}.getType());
    }

    public void sell(String seller, String role, String vin, String buyer) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("sell");
        request.setArgs(new String[]{seller, role, vin, buyer});
        executeTrx(request, hfc.getChain());
    }

    public void acceptInsurance(String username, String role, String userToInsure, String vin, String company) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("insuranceAccept");
        request.setArgs(new String[]{username, role, userToInsure, vin, company});
        executeTrx(request, hfc.getChain());
    }

    public void createUser(String username, String role, String newUser) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("createUser");
        request.setArgs(new String[]{username, role, newUser});
        executeTrx(request, hfc.getChain());
    }

    public Insurer getInsurer(String username, String role, String company) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role, company});
        request.setFcn("getInsurer");

        return query(request, hfc.getChain(), new TypeToken<Insurer>(){}.getType());
    }

    public void insureProposal(String username, String role, String vin, String company) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("insureProposal");
        request.setArgs(new String[]{username, role, vin, company});
        executeTrx(request, hfc.getChain());
    }

    public Map<Integer, Car> getCarHistory(String username, String role, String vin) throws Exception {
        QueryByChaincodeRequest request = hfc.getClient().newQueryProposalRequest();
        request.setArgs(new String[]{username, role, vin});
        request.setFcn("getHistory");

        return query(request, hfc.getChain(), new TypeToken<Map<Integer, Car>>(){}.getType());
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

    public void revocationProposal(String username, String role, String vin) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("revocationProposal");
        request.setArgs(new String[]{username, role, vin});
        executeTrx(request, hfc.getChain());
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

    public void createSellingOffer(String owner, String role, String price, String vin, String buyer) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("createSellingOffer");
        request.setArgs(new String[]{owner, role, price, vin, buyer});
        executeTrx(request, hfc.getChain());
    }

    public void importCar(String username, String role, Car car, ProposalData proposalData) throws Exception {
        TransactionProposalRequest request = hfc.getClient().newTransactionProposalRequest();
        request.setFcn("create");
        request.setArgs(new String[]{username, role, g.toJson(car), g.toJson(proposalData)});
        executeTrx(request, hfc.getChain());
    }

}
