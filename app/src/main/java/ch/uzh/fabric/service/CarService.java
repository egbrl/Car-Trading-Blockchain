package ch.uzh.fabric.service;

import ch.uzh.fabric.model.*;
import com.google.gson.reflect.TypeToken;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Service
public class CarService extends HFCService {

    public Collection<Car> getCars(HFClient client, Chain chain, String username, String role) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setArgs(new String[]{username, role});
        request.setFcn("readUser");

        ArrayList<Car> cars = new ArrayList<>();
        User user = query(request, chain, new TypeToken<User>(){}.getType());
        for (String vin : user.getCars()) {
            Car car = getCar(client, chain, username, role, vin);
            cars.add(car);
        }

        return cars;
    }

    public Collection<Offer> getSalesOffers(HFClient client, Chain chain, String username, String role) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setArgs(new String[]{username, role});
        request.setFcn("readUser");

        User user = query(request, chain, new TypeToken<User>(){}.getType());
        return user.getOffers();
    }

    public Collection<ProposalData> getRegistrationProposals(HFClient client, Chain chain, String username, String role) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setArgs(new String[]{username, role});
        request.setFcn("readRegistrationProposalsAsList");

        return query(request, chain, new TypeToken<Collection<ProposalData>>(){}.getType());
    }

    public void register(HFClient client, Chain chain, String owner, String role, String vin) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("register");
        request.setArgs(new String[]{owner, role, vin});
        executeTrx(request, chain);
    }

    public Car getCar(HFClient client, Chain chain, String username, String role, String vin) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setArgs(new String[]{username, role, vin});
        request.setFcn("readCar");

        return query(request, chain, new TypeToken<Car>(){}.getType());
    }

    public void sell(HFClient client, Chain chain, String seller, String role, String vin, String buyer) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("sell");
        request.setArgs(new String[]{seller, role, vin, buyer});
        executeTrx(request, chain);
    }

    public void acceptInsurance(HFClient client, Chain chain, String username, String role, String userToInsure, String vin, String company) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("insuranceAccept");
        request.setArgs(new String[]{username, role, userToInsure, vin, company});
        executeTrx(request, chain);
    }

    public void createUser(HFClient client, Chain chain, String username, String role, String newUser) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("createUser");
        request.setArgs(new String[]{username, role, newUser});
        executeTrx(request, chain);
    }

    public Insurer getInsurer(HFClient client, Chain chain, String username, String role, String company) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setArgs(new String[]{username, role, company});
        request.setFcn("getInsurer");

        return query(request, chain, new TypeToken<Insurer>(){}.getType());
    }

    public void insureProposal(HFClient client, Chain chain, String username, String role, String vin, String company) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("insureProposal");
        request.setArgs(new String[]{username, role, vin, company});
        executeTrx(request, chain);
    }

    public Map<Integer, Car> getCarHistory(HFClient client, Chain chain, String username, String role, String vin) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setArgs(new String[]{username, role, vin});
        request.setFcn("getHistory");

        return query(request, chain, new TypeToken<Map<Integer, Car>>(){}.getType());
    }

    public Collection<Car> getCarsToConfirm(HFClient client, Chain chain, String username, String role) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setFcn("getCarsToConfirmAsList");
        request.setArgs(new String[]{username, role});

        Collection<Car> result = query(request, chain, new TypeToken<Collection<Car>>(){}.getType());
        if (result == null) {
            result = new ArrayList<Car>();
        }

        return result;
    }

    public Collection<Car> getAllCars(HFClient client, Chain chain, String username, String role) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setFcn("getAllCarsAsList");
        request.setArgs(new String[]{username, role});

        Collection<Car> result = query(request, chain, new TypeToken<Collection<Car>>(){}.getType());
        if (result == null) {
            result = new ArrayList<Car>();
        }

        return result;
    }

    public void confirm(HFClient client, Chain chain, String username, String role, String vin, String numberplate) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("confirm");
        request.setArgs(new String[]{username, role, vin, numberplate});
        executeTrx(request, chain);
    }

    public void revocationProposal(HFClient client, Chain chain, String username, String role, String vin) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("revocationProposal");
        request.setArgs(new String[]{username, role, vin});
        executeTrx(request, chain);
    }

    public Map<String, String> getRevocationProposals(HFClient client, Chain chain, String username, String role) throws Exception {
        QueryByChaincodeRequest request = client.newQueryProposalRequest();
        request.setFcn("getRevocationProposals");
        request.setArgs(new String[]{username, role});

        return query(request, chain, new TypeToken<Map<String, String>>(){}.getType());
    }

    public void revoke(HFClient client, Chain chain, String owner, String role, String vin) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("revoke");
        request.setArgs(new String[]{owner, role, vin});
        executeTrx(request, chain);
    }

    public void createSellingOffer(HFClient client, Chain chain, String owner, String role, String price, String vin, String buyer) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("createSellingOffer");
        request.setArgs(new String[]{owner, role, price, vin, buyer});
        executeTrx(request, chain);
    }

    public void importCar(HFClient client, Chain chain, String username, String role, Car car, ProposalData proposalData) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("create");
        request.setArgs(new String[]{username, role, g.toJson(car), g.toJson(proposalData)});
        executeTrx(request, chain);
    }

}
