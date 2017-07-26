package ch.uzh.fabric.service;

import ch.uzh.fabric.config.ErrorInfo;
import ch.uzh.fabric.controller.AppController;
import ch.uzh.fabric.model.Car;
import ch.uzh.fabric.model.Insurer;
import ch.uzh.fabric.model.ProposalData;
import ch.uzh.fabric.model.User;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.grpc.StatusRuntimeException;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class CarService extends HFCService {

    private Gson g = new GsonBuilder().create();

    private ChainCodeID chainCodeID = ChainCodeID.newBuilder()
                                                 .setName(AppController.CHAIN_CODE_NAME)
                                                 .setVersion(AppController.CHAIN_CODE_VERSION)
                                                 .setPath(AppController.CHAIN_CODE_PATH).build();

    public HashMap<String, Car> getCars(HFClient client, Chain chain, String username, String role) {
        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(AppController.CHAIN_CODE_NAME)
                .setVersion(AppController.CHAIN_CODE_VERSION)
                .setPath(AppController.CHAIN_CODE_PATH).build();

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[]{username, role});
        queryByChaincodeRequest.setFcn("readUser");
        queryByChaincodeRequest.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
        } catch (InvalidArgumentException | ProposalException e) {
            throw new CompletionException(e);
        }

        User user = null;
        HashMap<String, Car> carList = new HashMap<>();
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
                        + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
                System.out.println(result.errorMessage.toString());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                user = g.fromJson(payload, User.class);
                for (String vin : user.getCars()) {
                    carList.put(vin, new Car(null, 0, vin));
                }

                //System.out.println("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
            }
        }

        for (Car car : carList.values()) {
            QueryByChaincodeRequest carRequest = client.newQueryProposalRequest();
            carRequest.setArgs(new String[]{username, role, car.getVin()});
            carRequest.setFcn("readCar");
            carRequest.setChaincodeID(chainCodeID);

            Collection<ProposalResponse> carQueryProps;
            try {
                carQueryProps = chain.queryByChaincode(carRequest);
            } catch (InvalidArgumentException | ProposalException e) {
                throw new CompletionException(e);
            }

            for (ProposalResponse proposalResponse : carQueryProps) {
                if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                    ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
                            + ". Messages: " + proposalResponse.getMessage()
                            + ". Was verified : " + proposalResponse.isVerified());
                    System.out.println(result.errorMessage.toString());
                } else {
                    String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                    car = g.fromJson(payload, Car.class);
                    carList.replace(car.getVin(), car);
                    //System.out.println("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                }
            }
        }

        return carList;
    }

    public Car getCar(HFClient client, Chain chain, String username, String role, String vin) {
        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(AppController.CHAIN_CODE_NAME)
                .setVersion(AppController.CHAIN_CODE_VERSION)
                .setPath(AppController.CHAIN_CODE_PATH).build();

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[]{username, role, vin});
        queryByChaincodeRequest.setFcn("readCar");
        queryByChaincodeRequest.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
        } catch (InvalidArgumentException | ProposalException e) {
            throw new CompletionException(e);
        }

        Car car = null;
        HashMap<String, Car> carList = new HashMap<>();
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
                        + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
                System.out.println(result.errorMessage.toString());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                car = g.fromJson(payload, Car.class);
            }
        }

        return car;
    }

    public void acceptInsurance(HFClient client, Chain chain, String username, String role, String userToInsure, String vin, String company) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("insuranceAccept");
        request.setArgs(new String[]{username, role, userToInsure, vin, company});
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
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setChaincodeID(chainCodeID);
        request.setFcn("revoke");
        request.setArgs(new String[]{owner, role, vin});

        Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(request, chain.getPeers());

        for (ProposalResponse response : invokePropResp) {
            if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        if (failed.size() > 0) {
            String error = failed.iterator().next().getMessage();
            String msg = error.substring(error.indexOf("message: ") + 9, error.indexOf("), cause"));
            throw new ProposalException(msg);
        }

        chain.sendTransaction(successful).get(AppController.TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
    }

    public void createOffer(HFClient client, Chain chain, String owner, String role, String price, String vin, String buyer) throws Exception {
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setChaincodeID(chainCodeID);
        request.setFcn("createSellingOffer");
        request.setArgs(new String[]{owner, role, price, vin, buyer});

        Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(request, chain.getPeers());

        for (ProposalResponse response : invokePropResp) {
            if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                successful.add(response);
            } else {
                failed.add(response);
            }
        }

        if (failed.size() > 0) {
            String error = failed.iterator().next().getMessage();
            String msg = error.substring(error.indexOf("message: ") + 9, error.indexOf("), cause"));
            throw new ProposalException(msg);
        }

        chain.sendTransaction(successful).get(AppController.TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
    }

    public void importCar(HFClient client, Chain chain, String username, String role, Car car, ProposalData proposalData) throws Exception {
        TransactionProposalRequest request = client.newTransactionProposalRequest();
        request.setFcn("create");
        request.setArgs(new String[]{username, role, g.toJson(car), g.toJson(proposalData)});
        executeTrx(request, chain);
    }

}
