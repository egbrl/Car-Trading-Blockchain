package ch.uzh.fabric.service;

import ch.uzh.fabric.config.ErrorInfo;
import ch.uzh.fabric.controller.AppController;
import ch.uzh.fabric.model.Car;
import ch.uzh.fabric.model.User;
import com.google.gson.*;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.ProposalException;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletionException;

public class DotCarService {

    private Gson g = new GsonBuilder().create();

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
}
