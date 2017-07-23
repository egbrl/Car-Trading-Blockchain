package ch.uzh.fabric.controller;

import ch.uzh.fabric.config.*;
import ch.uzh.fabric.model.*;
import ch.uzh.fabric.service.CarService;
import com.google.gson.*;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

@Controller
public class AppController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppController.class);
    private static final TestConfig TESTCONFIG = TestConfig.getConfig();
    private static final String TEST_ADMIN_NAME = "admin";
    private static final String TESTUSER_1_NAME = "user1";
    private static final String FOO_CHAIN_NAME = "foo";
    private static final String PROJECT_ROOT = "/var/egabb/";
    public static final String CHAIN_CODE_NAME = "car_cc_go";
    public static final String CHAIN_CODE_PATH = "github.com/car_cc";
    public static final String CHAIN_CODE_VERSION = "1";

    private static final String TEST_VIN = "WVWZZZ6RZHY260780";

    private Collection<SampleOrg> testSampleOrgs;
    private SampleStore sampleStore;
    private HFClient client;
    private Chain chain;

    private CarService carService = new CarService();

    private JsonSerializer<Date> ser = new JsonSerializer<Date>() {
        @Override
        public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext
                context) {
            return src == null ? null : new JsonPrimitive(src.getTime());
        }
    };

    private JsonDeserializer<Date> deser = new JsonDeserializer<Date>() {
        @Override
        public Date deserialize(JsonElement json, Type typeOfT,
                                JsonDeserializationContext context) throws JsonParseException {
            return json == null ? null : new Date(json.getAsLong());
        }
    };

    Gson g = null;

    @Autowired
    private ProfileProperties profileProperties;


	/*
     *	URL MAPPINGS
	 *
	 */

    @RequestMapping("/")
    public String root(Authentication authentication) {
        try {
            authentication.isAuthenticated();
            return "redirect:/index";
        } catch (Exception e) {
            return "redirect:/login";
        }
    }

    @RequestMapping(value = "/account", method = RequestMethod.GET)
    public String account(Authentication authentication, Model model) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        ProfileProperties.User user = null;

        // look for an existing user profile
        for (ProfileProperties.User userProperties : profileProperties.getUsers()) {
            if (userProperties.getName().equals(username) && userProperties.getRole().equals(role)) {
                user = userProperties;
            }
        }

        // if no settings for this user found,
        // create a new profile and append it to the list
        // of global user profiles
        if (user == null) {
            user = new ProfileProperties.User();
            user.setName(username);
            user.setRole(role);

            List<ProfileProperties.User> users = profileProperties.getUsers();
            users.add(user);
            profileProperties.setUsers(users);
        }

        model.addAttribute("orgName", user.getOrganization());
        model.addAttribute("role", role.toUpperCase());
        return "account";
    }

    @RequestMapping(value = "/account", method = RequestMethod.POST)
    public String accountChange(Authentication authentication, Model model, @RequestParam("orgName") String orgName) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        ProfileProperties.User user = null;
        for (ProfileProperties.User userProps : profileProperties.getUsers()) {
            if (userProps.getName().equals(username) && userProps.getRole().equals(role)) {
                userProps.setOrganization(orgName);
                user = userProps;
            }
        }

        // we can guarantee that 'user' is not 'null', i.e. exists
        // because we create the user in the 'GET' request already
        model.addAttribute("orgName", user.getOrganization());
        model.addAttribute("role", role.toUpperCase());
        return "account";
    }

    @RequestMapping("/index")
    public String index(Authentication authentication, Model model) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString();

        // Redirect to DOT and INSURANCE index page
        if (role.equals("ROLE_dot")) {
            try {
                authentication.isAuthenticated();
                return "redirect:/dot/registration";
            } catch (Exception e) {
                return "redirect:/login";
            }
        } else if (role.equals("ROLE_insurer")) {
            try {
                authentication.isAuthenticated();
                return "redirect:/insurance/index";
            } catch (Exception e) {
                return "redirect:/login";
            }
        }

        HashMap<String, Car> carList = carService.getCars(client, chain, username, role);

        model.addAttribute("cars", carList.values());
        model.addAttribute("role", role.substring(5).toUpperCase());
        return "index";
    }

    @RequestMapping(value = "/import", method = RequestMethod.GET)
    public String showImportForm(Model model, Authentication authentication, @ModelAttribute("car") Car carData, @ModelAttribute("proposalData") ProposalData proposalData) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        model.addAttribute("role", role.toUpperCase());
        return "import";
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public String createCar(Model model, Authentication authentication, @ModelAttribute("car") Car carData, @ModelAttribute("proposalData") ProposalData proposalData) {
        proposalData.setCar(carData.getVin());

        out("Vin: " + carData.getVin() + " _Brand: " + carData.getCertificate().getBrand());
        out("Doors:" + proposalData.getNumberOfDoors() + "_max speed: " + proposalData.getMaxSpeed());

        String username;
        String garageRole;

        try {
            // Authenticated web app request
            username = authentication.getName();
            // Role should only be "garage", if security is configured correctly
            // garageRole = SecurityConfig.BOOTSTRAP_GARAGE_ROLE;
            garageRole = authentication.getAuthorities().toArray()[0].toString().substring(5);
            out("read username and role from web request");
        } catch (NullPointerException e) {
            // Can only be the bootstrap script
            username = SecurityConfig.BOOTSTRAP_GARAGE_USER;
            garageRole = SecurityConfig.BOOTSTRAP_GARAGE_ROLE;
            out("read username and role from bootstraped code values");
        }

        out(username);
        out(garageRole);

        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        try {
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chainCodeID);
            transactionProposalRequest.setFcn("create");

            transactionProposalRequest.setArgs(new String[]{username, garageRole, g.toJson(carData), g.toJson(proposalData)});
            out("sending transaction proposal to 'create' a car to all peers");

            Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(transactionProposalRequest, chain.getPeers());
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    invokePropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                throw new ProposalException("Not enough endorsers for invoke");

            }
            out("Successfully received transaction proposal responses.");

            out("Sending chain code transaction to orderer");
            chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            out(e.toString());
            e.printStackTrace();
            ErrorInfo result = new ErrorInfo(500, "", "CompletionException " + e.getMessage());
            //return result;
            model.addAttribute("error", "Choose another VIN");
        }

        try {
            model.addAttribute("role", garageRole.toUpperCase());
        } catch (NullPointerException e) {
            // It's ok, we are in bootstrap mode..
        }

        return "import";
    }

    @RequestMapping("/login")
    public String login() {
        return "login";
    }

    @RequestMapping("/login-error")
    public String loginError(Model model) {
        model.addAttribute("loginError", true);
        return "login";
    }

    @RequestMapping("/dot/registration")
    public String dotRegistration(Model model, Authentication authentication) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[]{username, role});
        queryByChaincodeRequest.setFcn("readRegistrationProposalsAsList");
        queryByChaincodeRequest.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
        } catch (InvalidArgumentException | ProposalException e) {
            throw new CompletionException(e);
        }

        HashMap<String, ProposalAndCar> proposalAndCarMap = new HashMap<>();
        ArrayList<ProposalData> proposalDataArraylist = new ArrayList<ProposalData>();
        Boolean noResponseData = false;

        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
                        + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
                out(result.errorMessage.toString());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                ProposalData[] arr = new ProposalData[0];
                arr = g.fromJson(payload, ProposalData[].class);
                try {
                    proposalDataArraylist = new ArrayList<ProposalData>(Arrays.asList(arr));
                    Iterator<ProposalData> iterator = proposalDataArraylist.iterator();
                    while (iterator.hasNext()) {
                        ProposalData proposalData = iterator.next();
                        ProposalAndCar proposalAndCar = new ProposalAndCar(proposalData, null);
                        proposalAndCarMap.put(proposalData.getCar(), proposalAndCar);
                    }
                } catch (Exception e) {
                    out("Exception caught: " + e.toString());
                    noResponseData = true;
                    out("No data in response payload.");
                }

                out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);


            }

        }


        if (!noResponseData) {
            for (String vin : proposalAndCarMap.keySet()) {
                QueryByChaincodeRequest carRequest = client.newQueryProposalRequest();
                carRequest.setArgs(new String[]{username, role, vin});
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
                        out(result.errorMessage.toString());
                    } else {
                        String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                        Car car = new Car(null, null, null);
                        car = g.fromJson(payload, Car.class);

                        ProposalAndCar proposalAndCarObject = proposalAndCarMap.get(vin);
                        proposalAndCarObject.setCar(car);

                        proposalAndCarMap.replace(vin, proposalAndCarObject);
                        out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
                    }
                }
            }
        }

        model.addAttribute("proposalAndCarData", proposalAndCarMap.values());
        model.addAttribute("noProposals", noResponseData);
        model.addAttribute("role", role.toUpperCase());


        return "dot/registration";
    }

    @RequestMapping(value = "/dot/registration/accept", method = RequestMethod.POST)
    public String acceptRegistration(RedirectAttributes redirAttr, Authentication authentication, @RequestParam("vin") String vin) {
        out("vin" + vin);
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        try {
            ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(AppController.CHAIN_CODE_NAME)
                    .setVersion(AppController.CHAIN_CODE_VERSION)
                    .setPath(AppController.CHAIN_CODE_PATH).build();

            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chainCodeID);
            transactionProposalRequest.setFcn("register");

            transactionProposalRequest.setArgs(new String[]{username, role, vin});
            out("sending transaction proposal for 'register' to all peers");

            Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(transactionProposalRequest, chain.getPeers());
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    invokePropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                throw new ProposalException("Not enough endorsers for invoke");

            }
            out("Successfully received transaction proposal responses.");
            out("Sending chain code transaction to orderer");
            chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            out(e.toString());
            e.printStackTrace();
        }

        // redirAttr.addAttribute("success", "Registration proposal accepted. Car with Vin: '"+vin+"' is successfully registered.");
        return "redirect:/dot/registration";
    }

    @RequestMapping("/dot/confirmation")
    public String dotConfirmation(Model model, Authentication authentication, @RequestParam(required = false) String confirmSuccess, @RequestParam(required = false) String confirmFail) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[]{username, role});
        queryByChaincodeRequest.setFcn("getCarsToConfirmAsList");
        queryByChaincodeRequest.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
        } catch (InvalidArgumentException | ProposalException e) {
            throw new CompletionException(e);
        }

        ArrayList<Car> carstoConfirmAsList = new ArrayList<Car>();
        Boolean noResponseData = false;

        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
                        + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
                out(result.errorMessage.toString());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                Car[] arr = new Car[0];
                arr = g.fromJson(payload, Car[].class);
                try {
                    carstoConfirmAsList = new ArrayList<Car>(Arrays.asList(arr));
                } catch (Exception e) {
                    out("Exception caught: " + e.toString());
                    noResponseData = true;
                    out("No data in response payload.");
                }
                out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
            }
        }

        out("Size of carsToConfirmList: " + carstoConfirmAsList.size());
        model.addAttribute("confirmSuccess", confirmSuccess);
        model.addAttribute("confirmFail", confirmFail);
        model.addAttribute("carsToConfirmList", carstoConfirmAsList);
        model.addAttribute("noResponseData", noResponseData);
        model.addAttribute("role", role.toUpperCase());

        return "dot/confirmation";
    }

    @RequestMapping("/dot/confirmation/confirmcar")
    public String confirmCar(RedirectAttributes redirAttr, Authentication authentication, @RequestParam("vin") String vin, @RequestParam("numberplate") String numberplate) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        try {
            ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(AppController.CHAIN_CODE_NAME)
                    .setVersion(AppController.CHAIN_CODE_VERSION)
                    .setPath(AppController.CHAIN_CODE_PATH).build();



            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chainCodeID);
            transactionProposalRequest.setFcn("confirm");

            transactionProposalRequest.setArgs(new String[]{username, role, vin, numberplate});
            out("sending transaction proposal for 'confirm' to all peers");

            Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(transactionProposalRequest, chain.getPeers());
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    invokePropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                throw new ProposalException("Not enough endorsers for invoke");

            }
            out("Successfully received transaction proposal responses.");
            out("Sending chain code transaction to orderer");
            chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            out(e.toString());
            e.printStackTrace();
        }

        if (failed.size() > 0) {
            redirAttr.addAttribute("confirmFail", "Numberplate already taken. Confirmation for car '" + vin + "' with numberplate '" + numberplate + "' failed.");
        } else {
            redirAttr.addAttribute("confirmSuccess", "Confirmation for car '" + vin + "' with numberplate '" + numberplate + "' successful.");
        }
        return "redirect:/dot/confirmation";
    }

    @RequestMapping("/dot/all-cars")
    public String allCars(Authentication authentication, Model model) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString();

        HashMap<String, Car> carList = carService.getCars(client, chain, username, role);

        model.addAttribute("cars", carList.values());
        model.addAttribute("role", role.substring(5).toUpperCase());
        return "index";
    }

    @RequestMapping("/insurance/index")
    public String insuranceIndex(Model model, Authentication authentication, @RequestParam(required = false) String success) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(AppController.CHAIN_CODE_NAME)
                .setVersion(AppController.CHAIN_CODE_VERSION)
                .setPath(AppController.CHAIN_CODE_PATH).build();

        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();

        String companyName = ProfileProperties.getOrganization(profileProperties, username, role);
        queryByChaincodeRequest.setArgs(new String[]{username, role, companyName});
        queryByChaincodeRequest.setFcn("getInsurer");
        queryByChaincodeRequest.setChaincodeID(chainCodeID);

        Collection<ProposalResponse> queryProposals;

        try {
            queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
        } catch (InvalidArgumentException | ProposalException e) {
            throw new CompletionException(e);
        }

        Insurer insurer = null;
        for (ProposalResponse proposalResponse : queryProposals) {
            if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
                ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
                        + ". Messages: " + proposalResponse.getMessage()
                        + ". Was verified : " + proposalResponse.isVerified());
                System.out.println(result.errorMessage.toString());
            } else {
                String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
                insurer = g.fromJson(payload, Insurer.class);
                out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
            }
        }

        try {

            for (InsureProposal proposal : insurer.getProposals()) {
                out("IsRegistered before checking: " + proposal.isRegistered());
                Car car = carService.getCar(client, chain, proposal.getUser(), "user", proposal.getCar());
                proposal.setRegistered(car.isRegistered());
            }

            for (InsureProposal proposal : insurer.getProposals()) {
                out("IsRegistered after checking: " + proposal.isRegistered());
            }

        } catch (NullPointerException e) {
            // Insurer not yet created, because no proposals on this insurer exist
            insurer = new Insurer(ProfileProperties.getOrganization(profileProperties, username, role), null);
        }


        model.addAttribute("success", success);
        model.addAttribute("role", role.toUpperCase());
        model.addAttribute("insurer", insurer);
        return "insurance/index";
    }

    @RequestMapping("/insurance/acceptInsurance")
    public String acceptInsurance(RedirectAttributes redirAttr, Authentication authentication, @RequestParam("vin") String vin, @RequestParam("userToInsure") String userToInsure) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);
        String company = ProfileProperties.getOrganization(profileProperties, username, role);

        try {
            ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(AppController.CHAIN_CODE_NAME)
                    .setVersion(AppController.CHAIN_CODE_VERSION)
                    .setPath(AppController.CHAIN_CODE_PATH).build();

            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chainCodeID);
            transactionProposalRequest.setFcn("insuranceAccept");

            transactionProposalRequest.setArgs(new String[]{username, role, userToInsure, vin, company});
            out("sending transaction proposal for 'insuranceAccept' to all peers");

            Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(transactionProposalRequest, chain.getPeers());
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    invokePropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                throw new ProposalException("Not enough endorsers for invoke");

            }
            out("Successfully received transaction proposal responses.");
            out("Sending chain code transaction to orderer");
            chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            out(e.toString());
            e.printStackTrace();
        }

        redirAttr.addAttribute("success", "Insurance proposal accepted. '" + company + "' now insures car '" + vin + "' of user '" + userToInsure + "'.");
        return "redirect:/insurance/index";
    }

    @RequestMapping(value = "/insure", method = RequestMethod.GET)
    public String showInsureForm(Model model, Authentication authentication, @RequestParam(required = false) String success, @RequestParam(required = false) String activeVin) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);
        HashMap<String, Car> carList = carService.getCars(client, chain, username, role);

        model.addAttribute("activeVin", activeVin);
        model.addAttribute("success", success);
        model.addAttribute("cars", carList.values());
        model.addAttribute("role", role.toUpperCase());
        return "insure";
    }

    @RequestMapping(value = "/insure", method = RequestMethod.POST)
    public String insuranceProposal(RedirectAttributes redirAttr, Authentication authentication, @RequestParam("vin") String vin, @RequestParam("company") String company) {
        out(vin);
        out(company);

        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);

        out(username);
        out(role);

        ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        try {
            Collection<ProposalResponse> successful = new LinkedList<>();
            Collection<ProposalResponse> failed = new LinkedList<>();

            TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
            transactionProposalRequest.setChaincodeID(chainCodeID);
            transactionProposalRequest.setFcn("insureProposal");

            transactionProposalRequest.setArgs(new String[]{username, role, vin, company});
            out("sending transaction proposal for 'insureProposal' to all peers");

            Collection<ProposalResponse> invokePropResp = chain.sendTransactionProposal(transactionProposalRequest, chain.getPeers());
            for (ProposalResponse response : invokePropResp) {
                if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
                    out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                    successful.add(response);
                } else {
                    failed.add(response);
                }
            }
            out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
                    invokePropResp.size(), successful.size(), failed.size());
            if (failed.size() > 0) {
                throw new ProposalException("Not enough endorsers for invoke");

            }
            out("Successfully received transaction proposal responses.");

            out("Sending chain code transaction to orderer");
            chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
        } catch (Exception e) {
            out(e.toString());
            e.printStackTrace();
            ErrorInfo result = new ErrorInfo(500, "", "CompletionException " + e.getMessage());
            //return result;
            //model.addAttribute("error", "");
        }

        redirAttr.addAttribute("success", "Insurance proposal saved. '" + company + "' will get back to you for confirmation.");
        return "redirect:/insure";
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public String history(Model model, Authentication authentication, @RequestParam String vin) {
        String username = authentication.getName();
        String role = authentication.getAuthorities().toArray()[0].toString().substring(5);
        Car[] history = carService.getCarHistory(client, chain, username, role, vin);

        model.addAttribute("vin", vin);
        model.addAttribute("cars", history);
        model.addAttribute("role", role.toUpperCase());
        return "history";
    }

	/*
     *	INITIALIZE FUNCTIONS
	 *
	 */

    @PostConstruct
    public void AppController() throws Exception {
        System.out.println("╔═╗┌─┐┌┐ ┬─┐┬┌─┐  ╔╗ ┌─┐┌─┐┌┬┐┌─┐┌┬┐┬─┐┌─┐┌─┐\n" +
                "╠╣ ├─┤├┴┐├┬┘││    ╠╩╗│ ││ │ │ └─┐ │ ├┬┘├─┤├─┘\n" +
                "╚  ┴ ┴└─┘┴└─┴└─┘  ╚═╝└─┘└─┘ ┴ └─┘ ┴ ┴└─┴ ┴┴  ");

        initSampleStore();
        setupclient();
        getconfig();

        enrolladmin();
        enrollusers();
        enrollorgadmin();
        constructchain();
        installchaincode();
        instantiatechaincode();

        g = new GsonBuilder()
                .registerTypeAdapter(Date.class, ser)
                .registerTypeAdapter(Date.class, deser).create();

        // Create first garage user car
        createCar(null, null, new Car(
                        new Certificate(
                                null,
                                null,
                                null,
                                null,
                                "white",
                                "C350",
                                "Mercedes"), null, TEST_VIN),
                new ProposalData(
                        "ZH 1234",
                        "4+1",
                        4,
                        2,
                        200)
        );

        System.out.println("Hyperledger network is ready to use");
    }

    private void initSampleStore() {
        File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");
        sampleStore = new SampleStore(sampleStoreFile);
        LOGGER.info(sampleStore.toString());
    }

    private ErrorInfo setupclient() {
        client = HFClient.createNewInstance();
        try {
            client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        } catch (CryptoException ex) {
            LOGGER.error("CryptoException setting crypto suite", ex);
            ErrorInfo result = new ErrorInfo(500, "", "CryptoException setting crypto suite");
            return result;
        } catch (InvalidArgumentException ex) {
            LOGGER.error("InvalidArgumentException setting crypto suite", ex);
            ErrorInfo result = new ErrorInfo(500, "", "InvalidArgumentException setting crypto suite");
            return result;
        }
        ErrorInfo result = new ErrorInfo(0, "", "OK");
        return result;
    }

    private ErrorInfo getconfig() {
        testSampleOrgs = TESTCONFIG.getIntegrationTestsSampleOrgs();

        for (SampleOrg sampleOrg : testSampleOrgs) {
            try {
                sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
            } catch (MalformedURLException ex) {
                LOGGER.error("MalformedURLException setting client config", ex);
                ErrorInfo result = new ErrorInfo(500, "", "MalformedURLException setting client config");
                return result;
            }
        }

        ErrorInfo result = new ErrorInfo(0, "", "OK");
        return result;
    }

    private List<EnrollAdminResponse> enrolladmin() throws Exception {

        List<EnrollAdminResponse> result = new ArrayList<>();

        for (SampleOrg sampleOrg : testSampleOrgs) {
            HFCAClient ca = sampleOrg.getCAClient();
            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMSPID();
            ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
            SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
            if (!admin.isEnrolled()) {
                admin.setEnrollment(ca.enroll(admin.getName(), "adminpw"));
                admin.setMPSID(mspid);
            }

            sampleOrg.setAdmin(admin);
            EnrollAdminResponse resp = new EnrollAdminResponse(admin.getName(), orgName, admin.getMSPID(), admin.isEnrolled(), admin.isRegistered());
            result.add(resp);
            sampleStore.putMember(TEST_ADMIN_NAME, orgName, admin);
        }

        LOGGER.info(sampleStore.toString());
        return result;
    }

    private List<EnrollAdminResponse> enrollusers() throws Exception {
        List<EnrollAdminResponse> result = new ArrayList<>();

        for (SampleOrg sampleOrg : testSampleOrgs) {

            HFCAClient ca = sampleOrg.getCAClient();
            final String orgName = sampleOrg.getName();
            final String mspid = sampleOrg.getMSPID();
            SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);

            SampleUser user = sampleStore.getMember(TESTUSER_1_NAME, sampleOrg.getName());
            if (!user.isRegistered()) {
                RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
                user.setEnrollmentSecret(ca.register(rr, admin));
            }
            if (!user.isEnrolled()) {
                user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
                user.setMPSID(mspid);
            }
            sampleOrg.addUser(user);
            EnrollAdminResponse resp = new EnrollAdminResponse(user.getName(), orgName, user.getMSPID(), user.isEnrolled(), user.isRegistered());
            result.add(resp);
            sampleStore.putMember(TESTUSER_1_NAME, orgName, user);
        }

        LOGGER.info(sampleStore.toString());
        LOGGER.info(result.toString());
        return result;
    }

    private List<EnrollAdminResponse> enrollorgadmin() throws Exception {
        List<EnrollAdminResponse> result = new ArrayList<>();

        for (SampleOrg sampleOrg : testSampleOrgs) {
            final String orgName = sampleOrg.getName();
            final String sampleOrgName = sampleOrg.getName();
            final String sampleOrgDomainName = sampleOrg.getDomainName();

            SampleUser peerOrgAdmin = sampleStore.getMember(sampleOrgName + "Admin", sampleOrgName, sampleOrg.getMSPID(),
                    findFile_sk(Paths.get(TESTCONFIG.getTestChannlePath(), "crypto-config/peerOrganizations/",
                            sampleOrgDomainName, format("/users/Admin@%s/msp/keystore", sampleOrgDomainName)).toFile()),
                    Paths.get(TESTCONFIG.getTestChannlePath(), "crypto-config/peerOrganizations/", sampleOrgDomainName,
                            format("/users/Admin@%s/msp/signcerts/Admin@%s-cert.pem", sampleOrgDomainName, sampleOrgDomainName)).toFile());

            //A special user that can crate channels, join peers and install chain code
            sampleOrg.setPeerAdmin(peerOrgAdmin);

            EnrollAdminResponse resp = new EnrollAdminResponse(peerOrgAdmin.getName(), orgName, peerOrgAdmin.getMSPID(), peerOrgAdmin.isEnrolled(), peerOrgAdmin.isRegistered());
            result.add(resp);
        }

        return result;
    }

    private File findFile_sk(File directory) {
        File[] matches = directory.listFiles((dir, name) -> name.endsWith("_sk"));

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];
    }

    private ErrorInfo constructchain() throws InvalidArgumentException, IOException, TransactionException, ProposalException {
        SampleOrg sampleOrg = TESTCONFIG.getIntegrationTestsSampleOrg("peerOrg1");

        out("Constructing chain %s", FOO_CHAIN_NAME);

        Collection<Orderer> orderers = new LinkedList<>();

        for (String orderName : sampleOrg.getOrdererNames()) {
            orderers.add(client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
                    TESTCONFIG.getOrdererProperties(orderName)));
        }

        //Just pick the first orderer in the list to create the chain.
        Orderer anOrderer = orderers.iterator().next();
        orderers.remove(anOrderer);

        ChainConfiguration chainConfiguration = new ChainConfiguration(new File(PROJECT_ROOT + "/fixtures/e2e-2Orgs/channel/" + FOO_CHAIN_NAME + ".tx"));

        //Only peer Admin org
        client.setUserContext(sampleOrg.getPeerAdmin());

        //Create chain that has only one signer that is this orgs peer admin. If chain creation policy needed more signature they would need to be added too.
        Chain newChain = client.newChain(FOO_CHAIN_NAME, anOrderer, chainConfiguration, client.getChainConfigurationSignature(chainConfiguration, sampleOrg.getPeerAdmin()));

        out("Created chain %s", FOO_CHAIN_NAME);

        out("Adding peers to chain");

        for (String peerName : sampleOrg.getPeerNames()) {
            String peerLocation = sampleOrg.getPeerLocation(peerName);

            //test properties for peer.. if any.
            Properties peerProperties = TESTCONFIG.getPeerProperties(peerName);
            if (peerProperties == null) {
                peerProperties = new Properties();
            }

            //Example of setting specific options on grpc's ManagedChannelBuilder
            peerProperties.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);

            Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
            newChain.joinPeer(peer);
            out("Peer %s joined chain %s", peerName, FOO_CHAIN_NAME);
            sampleOrg.addPeer(peer);
        }

        out("Adding remaining orderers to chain");

        //add remaining orderers if any.
        for (Orderer orderer : orderers) {
            newChain.addOrderer(orderer);
        }

        out("Adding event hubs to chain");

        for (String eventHubName : sampleOrg.getEventHubNames()) {
            EventHub eventHub = client.newEventHub(eventHubName, sampleOrg.getEventHubLocation(eventHubName),
                    TESTCONFIG.getEventHubProperties(eventHubName));
            newChain.addEventHub(eventHub);
        }

        out("starting chain intialisation");

        newChain.initialize();

        out("Finished initialization chain %s", FOO_CHAIN_NAME);

        chain = newChain;

        ErrorInfo result = new ErrorInfo(0, "", "OK");
        return result;
    }

    private ErrorInfo installchaincode() throws InvalidArgumentException, ProposalException {
        final String chainName = chain.getName();
        out("installing chaincode on chain %s", chainName);
        chain.setTransactionWaitTime(TESTCONFIG.getTransactionWaitTime());
        chain.setDeployWaitTime(TESTCONFIG.getDeployWaitTime());

        final ChainCodeID chainCodeID;
        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();

        chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        SampleOrg sampleOrg = TESTCONFIG.getIntegrationTestsSampleOrg("peerOrg1");
        client.setUserContext(sampleOrg.getPeerAdmin());

        out("Creating install proposal");

        InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(chainCodeID);

        // install from directory (install from stream also available)
        // For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
        installProposalRequest.setChaincodeSourceLocation(new File(PROJECT_ROOT + "/chaincode"));

        installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);

        out("Sending install proposal");

        // only a client from the same org as the peer can issue an install request
        int numInstallProposal = 0;
        //    Set<String> orgs = orgPeers.keySet();
        //   for (SampleOrg org : testSampleOrgs) {

        Set<Peer> peersFromOrg = sampleOrg.getPeers();
        numInstallProposal = numInstallProposal + peersFromOrg.size();
        responses = client.sendInstallProposal(installProposalRequest, peersFromOrg);

        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
                out("Successful install proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
                successful.add(response);
            } else {
                failed.add(response);
            }
        }
        //   }
        out("Received %d install proposal responses. Successful+verified: %d . Failed: %d", numInstallProposal, successful.size(), failed.size());

        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            ErrorInfo result = new ErrorInfo(500, "", "Not enough endorsers for install :" + successful.size() + ".  " + first.getMessage());
            return result;
        }

        ErrorInfo result = new ErrorInfo(0, "", "OK");
        return result;
    }

    // save the CC invoke TxID and use in queries
    String testTxID = null;

    private ErrorInfo instantiatechaincode() throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ProposalException, InterruptedException, ExecutionException, TimeoutException {

        final ChainCodeID chainCodeID;
        Collection<ProposalResponse> responses;
        Collection<ProposalResponse> successful = new LinkedList<>();
        Collection<ProposalResponse> failed = new LinkedList<>();
        Collection<Orderer> orderers = chain.getOrderers();

        chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION)
                .setPath(CHAIN_CODE_PATH).build();

        SampleOrg sampleOrg = TESTCONFIG.getIntegrationTestsSampleOrg("peerOrg1");
        client.setUserContext(sampleOrg.getPeerAdmin());

        // Instantiate chain code.
        InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(60000);
        instantiateProposalRequest.setChaincodeID(chainCodeID);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs(new String[]{"999"});
        Map<String, byte[]> tm = new HashMap<>();
        tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
        tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
        instantiateProposalRequest.setTransientMap(tm);

        // policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
        // See README.md Chaincode endorsement policies section for more details.
        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(PROJECT_ROOT + "/fixtures/chaincodeendorsementpolicy.yaml"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        out("Sending instantiateProposalRequest to all peers.");
        successful.clear();
        failed.clear();

        responses = chain.sendInstantiationProposal(instantiateProposalRequest, chain.getPeers());
        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
                successful.add(response);
                out("Succesful instantiate proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
            } else {
                failed.add(response);
            }
        }
        out("Received %d instantiate proposal responses. Successful+verified: %d . Failed: %d", responses.size(), successful.size(), failed.size());
        if (failed.size() > 0) {
            ProposalResponse first = failed.iterator().next();
            ErrorInfo result = new ErrorInfo(500, "", "Not enough endorsers for instantiate :" + successful.size() + "endorser failed with " + first.getMessage() + ". Was verified:" + first.isVerified());
            return result;
        }

        // Send instantiate transaction to orderer
        out("Sending instantiateTransaction to orderer.");
        chain.sendTransaction(successful, orderers).thenApply(transactionEvent -> {
            out("Finished instantiate transaction with transaction id %s", transactionEvent.getTransactionID());
            return null;
        }).exceptionally(e -> {
            if (e instanceof TransactionEventException) {
                BlockEvent.TransactionEvent te = ((TransactionEventException) e).getTransactionEvent();
                if (te != null) {
                    ErrorInfo result = new ErrorInfo(500, "", "Transaction with txid %s failed. %s" + te.getTransactionID() + e.getMessage());
                    return result;
                }
            }

            ErrorInfo result = new ErrorInfo(500, "", e.getMessage());
            return result;
        }).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);

        ErrorInfo result = new ErrorInfo(0, "", "OK");
        return result;
    }

    static void out(String format, Object... args) {
        System.err.flush();
        System.out.flush();
        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();
    }

}
