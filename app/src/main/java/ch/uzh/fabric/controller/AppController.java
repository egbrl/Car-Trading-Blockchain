package ch.uzh.fabric.controller;

import ch.uzh.fabric.config.*;
import ch.uzh.fabric.model.*;
import ch.uzh.fabric.model.User;
import com.google.gson.*;
import io.grpc.StatusRuntimeException;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.*;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

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
	private static final String CHAIN_CODE_NAME = "car_cc_go";
	private static final String CHAIN_CODE_PATH = "github.com/car_cc";
	private static final String CHAIN_CODE_VERSION = "1";

	private static final String TEST_VIN = "WVWZZZ6RZHY260780";

	private Collection<SampleOrg> testSampleOrgs;
	private SampleStore sampleStore;
	private HFClient client;
	private Chain chain;

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

	@RequestMapping("/index")
	public String index(Authentication authentication, Model model) {
		String username = authentication.getName();
		String role = authentication.getAuthorities().toArray()[0].toString();

		// Redirect to DOT and INSURANCE index page
		if(role.equals("ROLE_dot")) {
			try {
				authentication.isAuthenticated();
				return "redirect:/dot/index";
			} catch (Exception e) {
				return "redirect:/login";
			}
		} else if (role.equals("ROLE_insurance")) {
			try {
				authentication.isAuthenticated();
				return "redirect:/insurance/index";
			} catch (Exception e) {
				return "redirect:/login";
			}
		}

		ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
				.setVersion(CHAIN_CODE_VERSION)
				.setPath(CHAIN_CODE_PATH).build();

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
				out(result.errorMessage.toString());
			} else {
				String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
				user = g.fromJson(payload, User.class);
				for (String vin : user.getCars()) {
					carList.put(vin, new Car(null, null, vin));
				}

				out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
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
					out(result.errorMessage.toString());
				} else {
					String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
					car = g.fromJson(payload, Car.class);
					carList.replace(car.getVin(), car);
					out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
				}
			}
		}

		model.addAttribute("cars", carList.values());
		model.addAttribute("role", role.substring(5).toUpperCase());
		return "index";
	}

	@RequestMapping(value="/import", method=RequestMethod.GET)
	public String showImportForm(Model model, Authentication authentication, @ModelAttribute("car") Car carData, @ModelAttribute("proposalData") ProposalData proposalData) {
		String username = authentication.getName();
		String role = authentication.getAuthorities().toArray()[0].toString().substring(5);
		model.addAttribute("role", role.toUpperCase());
		return "import";
	}

	@RequestMapping(value="/import", method=RequestMethod.POST)
	public String createCar(Model model, Authentication authentication, @ModelAttribute("car") Car carData, @ModelAttribute("proposalData") ProposalData proposalData) {
		proposalData.setCar(carData.getVin());

		out(carData.toString());
		out(proposalData.toString());

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


	@RequestMapping("/dot/index")
	public String dot(Model model, Authentication authentication) {
		String username = authentication.getName();
		String role = authentication.getAuthorities().toArray()[0].toString();
		String roleArg = "dot";

		ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
				.setVersion(CHAIN_CODE_VERSION)
				.setPath(CHAIN_CODE_PATH).build();

		QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
		queryByChaincodeRequest.setArgs(new String[]{username, roleArg});
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

		for (ProposalResponse proposalResponse : queryProposals) {
			if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
				ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
						+ ". Messages: " + proposalResponse.getMessage()
						+ ". Was verified : " + proposalResponse.isVerified());
				out(result.errorMessage.toString());
			} else {
				String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
				ProposalData[] arr = g.fromJson(payload, ProposalData[].class);
				proposalDataArraylist = new ArrayList<ProposalData>(Arrays.asList(arr));
				Iterator<ProposalData> iterator = proposalDataArraylist.iterator();
				while (iterator.hasNext()) {
					ProposalData proposalData = iterator.next();
					ProposalAndCar proposalAndCar = new ProposalAndCar(proposalData, null);
					proposalAndCarMap.put(proposalData.getCar(), proposalAndCar);
				}
				out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
			}
		}

		for (String vin : proposalAndCarMap.keySet()) {
			QueryByChaincodeRequest carRequest = client.newQueryProposalRequest();
			carRequest.setArgs(new String[]{username, roleArg, vin});
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
					out("VIN:" + car.getVin());
					out("maxSpeed: " + proposalAndCarObject.getProposalData().toString());
					proposalAndCarObject.setCar(car);

					proposalAndCarMap.replace(vin, proposalAndCarObject);
					out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
				}
			}
		}

		out("Size of proposalAndCarMap: " + proposalAndCarMap.values().size());
		model.addAttribute("proposalAndCarData", proposalAndCarMap.values());
		model.addAttribute("role", role.substring(5).toUpperCase());


		return "dot/index";
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
