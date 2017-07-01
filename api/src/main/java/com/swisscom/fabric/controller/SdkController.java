package com.swisscom.fabric.controller;

import com.google.protobuf.InvalidProtocolBufferException;
import com.swisscom.fabric.config.EnrollAdminResponse;
import com.swisscom.fabric.config.ErrorInfo;
import com.swisscom.fabric.config.SampleOrg;
import com.swisscom.fabric.config.SampleStore;
import com.swisscom.fabric.config.SampleUser;
import com.swisscom.fabric.config.TestConfig;
import com.swisscom.fabric.exceptions.ServiceException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import static java.lang.String.format;
import java.net.MalformedURLException;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import org.apache.commons.codec.binary.Hex;
import org.hyperledger.fabric.protos.ledger.rwset.kvrwset.KvRwset;
import org.hyperledger.fabric.protos.peer.Query;
import org.hyperledger.fabric.sdk.BlockEvent;
import org.hyperledger.fabric.sdk.BlockInfo;
import static org.hyperledger.fabric.sdk.BlockInfo.EnvelopeType.TRANSACTION_ENVELOPE;
import org.hyperledger.fabric.sdk.BlockchainInfo;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.ChainCodeID;
import org.hyperledger.fabric.sdk.ChainCodeResponse;
import org.hyperledger.fabric.sdk.ChainConfiguration;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.EventHub;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryByChaincodeRequest;
import org.hyperledger.fabric.sdk.TransactionInfo;
import org.hyperledger.fabric.sdk.TransactionProposalRequest;
import org.hyperledger.fabric.sdk.TxReadWriteSetInfo;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.InvalidProtocolBufferRuntimeException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionEventException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin
@RestController
@RequestMapping("rest")
public class SdkController extends AbstractRestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SdkController.class);

  private Collection<SampleOrg> testSampleOrgs;

  private static final TestConfig TESTCONFIG = TestConfig.getConfig();

  private SampleStore sampleStore;

  private HFClient client;

  Chain chain;

  @PostConstruct
  public void initSampleStore() {
    //Persistence is not part of SDK. Sample file store is for demonstration purposes only!
    //   MUST be replaced with more robust application implementation  (Database, LDAP)
    File sampleStoreFile = new File(System.getProperty("java.io.tmpdir") + "/HFCSampletest.properties");

    //if (sampleStoreFile.exists()) { //For testing start fresh
    //  sampleStoreFile.delete();
    //}
    sampleStore = new SampleStore(sampleStoreFile);
    LOGGER.info(sampleStore.toString());
  }

  @RequestMapping(value = "/setupclient", method = RequestMethod.GET)
  public ErrorInfo setupclient() {
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

  @RequestMapping(value = "/getconfig", method = RequestMethod.GET)
  public ErrorInfo getconfig() {
    testSampleOrgs = TESTCONFIG.getIntegrationTestsSampleOrgs();
    //Set up hfca for each sample org

    for (SampleOrg sampleOrg : testSampleOrgs) {
      try {
        sampleOrg.setCAClient(HFCAClient.createNewInstance(sampleOrg.getCALocation(), sampleOrg.getCAProperties()));
        //LOGGER.info(sampleOrg.toString());
      } catch (MalformedURLException ex) {
        LOGGER.error("MalformedURLException setting client config", ex);
        ErrorInfo result = new ErrorInfo(500, "", "MalformedURLException setting client config");
        return result;
      }
    }

    ErrorInfo result = new ErrorInfo(0, "", "OK");
    return result;
  }

  private static final String TEST_ADMIN_NAME = "admin";

  @RequestMapping(value = "/enrolladmin", method = RequestMethod.GET)
  public List<EnrollAdminResponse> enrolladmin() throws EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException, Exception {

    List<EnrollAdminResponse> result = new ArrayList<>();

    for (SampleOrg sampleOrg : testSampleOrgs) {
      HFCAClient ca = sampleOrg.getCAClient();
      final String orgName = sampleOrg.getName();
      final String mspid = sampleOrg.getMSPID();
      ca.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
      SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);
      if (!admin.isEnrolled()) {  //Preregistered admin only needs to be enrolled with Fabric caClient.
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

  private static final String TESTUSER_1_NAME = "user1";

  @RequestMapping(value = "/enrollusers", method = RequestMethod.GET)
  public List<EnrollAdminResponse> enrollusers() throws EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException, Exception {
    List<EnrollAdminResponse> result = new ArrayList<>();

    for (SampleOrg sampleOrg : testSampleOrgs) {

      HFCAClient ca = sampleOrg.getCAClient();
      final String orgName = sampleOrg.getName();
      final String mspid = sampleOrg.getMSPID();
      SampleUser admin = sampleStore.getMember(TEST_ADMIN_NAME, orgName);

      SampleUser user = sampleStore.getMember(TESTUSER_1_NAME, sampleOrg.getName());
      if (!user.isRegistered()) {  // users need to be registered AND enrolled
        RegistrationRequest rr = new RegistrationRequest(user.getName(), "org1.department1");
        user.setEnrollmentSecret(ca.register(rr, admin));
      }
      if (!user.isEnrolled()) {
        user.setEnrollment(ca.enroll(user.getName(), user.getEnrollmentSecret()));
        user.setMPSID(mspid);
      }
      sampleOrg.addUser(user); //Remember user belongs to this Org
      EnrollAdminResponse resp = new EnrollAdminResponse(user.getName(), orgName, user.getMSPID(), user.isEnrolled(), user.isRegistered());
      result.add(resp);
      sampleStore.putMember(TESTUSER_1_NAME, orgName, user);
    }

    LOGGER.info(sampleStore.toString());
    LOGGER.info(result.toString());
    return result;
  }

  @RequestMapping(value = "/enrollorgadmin", method = RequestMethod.GET)
  public List<EnrollAdminResponse> enrollorgadmin() throws EnrollmentException, org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException, Exception {
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

      sampleOrg.setPeerAdmin(peerOrgAdmin); //A special user that can crate channels, join peers and install chain code
      // and jump tall blockchains in a single leap!
      EnrollAdminResponse resp = new EnrollAdminResponse(peerOrgAdmin.getName(), orgName, peerOrgAdmin.getMSPID(), peerOrgAdmin.isEnrolled(), peerOrgAdmin.isRegistered());
      result.add(resp);
    }

    return result;
  }

  private static final String FOO_CHAIN_NAME = "foo";
  private static final String PROJECT_ROOT = "/home/andi/Documents/UZH/Masterprojekt/Hackathon/Car-Trading-Blockchain";

  @RequestMapping(value = "/getchain", method = RequestMethod.GET)
  public ErrorInfo getchain() throws InvalidArgumentException, IOException, TransactionException, ProposalException {

    SampleOrg sampleOrg = TESTCONFIG.getIntegrationTestsSampleOrg("peerOrg1");

    out("Constructing chain %s", FOO_CHAIN_NAME);

    //Only peer Admin org
    client.setUserContext(sampleOrg.getPeerAdmin());

    //Create chain that has only one signer that is this orgs peer admin. If chain creation policy needed more signature they would need to be added too.
    Chain newChain = client.newChain(FOO_CHAIN_NAME);
    out("Retrieved chain %s", FOO_CHAIN_NAME);

    out("Adding remaining orderers to chain");
    for (String orderName : sampleOrg.getOrdererNames()) {
      Orderer orderer = client.newOrderer(orderName, sampleOrg.getOrdererLocation(orderName),
        TESTCONFIG.getOrdererProperties(orderName));
      newChain.addOrderer(orderer);
    }

    out("Adding peers to chain");

    for (String peerName : sampleOrg.getPeerNames()) {
      String peerLocation = sampleOrg.getPeerLocation(peerName);

      Properties peerProperties = TESTCONFIG.getPeerProperties(peerName);//test properties for peer.. if any.
      if (peerProperties == null) {
        peerProperties = new Properties();
      }
      //Example of setting specific options on grpc's ManagedChannelBuilder
      peerProperties.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);

      Peer peer = client.newPeer(peerName, peerLocation, peerProperties);
      newChain.addPeer(peer);
      sampleOrg.addPeer(peer);
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

  @RequestMapping(value = "/constructchain", method = RequestMethod.GET)
  public ErrorInfo constructchain() throws InvalidArgumentException, IOException, TransactionException, ProposalException {

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

      Properties peerProperties = TESTCONFIG.getPeerProperties(peerName);//test properties for peer.. if any.
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

    for (Orderer orderer : orderers) { //add remaining orderers if any.
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

  private static final String CHAIN_CODE_NAME = "car_cc_go";
  private static final String CHAIN_CODE_PATH = "github.com/car_cc";
  private static final String CHAIN_CODE_VERSION = "1";

  @RequestMapping(value = "/installchaincode", method = RequestMethod.GET)
  public ErrorInfo installchaincode() throws InvalidArgumentException, ProposalException {

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
    ////For GO language and serving just a single user, chaincodeSource is mostly likely the users GOPATH
    installProposalRequest.setChaincodeSourceLocation(new File(PROJECT_ROOT + "/chaincode"));

    installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);

    out("Sending install proposal");

    ////////////////////////////
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

  final int delta = 100;
  String testTxID = null;  // save the CC invoke TxID and use in queries

  @RequestMapping(value = "/instantiatechaincode", method = RequestMethod.GET)
  public ErrorInfo instantiatechaincode() throws InvalidArgumentException, IOException, ChaincodeEndorsementPolicyParseException, ProposalException, InterruptedException, ExecutionException, TimeoutException {

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

    ///////////////
    //// Instantiate chain code.
    InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
    instantiateProposalRequest.setProposalWaitTime(60000);
    instantiateProposalRequest.setChaincodeID(chainCodeID);
    instantiateProposalRequest.setFcn("init");
    instantiateProposalRequest.setArgs(new String[]{"init", "999"});
    Map<String, byte[]> tm = new HashMap<>();
    tm.put("HyperLedgerFabric", "InstantiateProposalRequest:JavaSDK".getBytes(UTF_8));
    tm.put("method", "InstantiateProposalRequest".getBytes(UTF_8));
    instantiateProposalRequest.setTransientMap(tm);

    /*
              policy OR(Org1MSP.member, Org2MSP.member) meaning 1 signature from someone in either Org1 or Org2
              See README.md Chaincode endorsement policies section for more details.
     */
    ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
    chaincodeEndorsementPolicy.fromYamlFile(new File(PROJECT_ROOT + "/fixtures/chaincodeendorsementpolicy.yaml"));
    instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

    out("Sending instantiateProposalRequest to all peers with arguments: a and b set to 100 and %s respectively", "" + (200 + delta));
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

    ///////////////
    /// Send instantiate transaction to orderer
    out("Sending instantiateTransaction to orderer with a and b set to 100 and %s respectively", "" + (200 + delta));
    chain.sendTransaction(successful, orderers).thenApply(transactionEvent -> {

      //waitOnFabric(0);
      //assertTrue(transactionEvent.isValid()); // must be valid to be here.
      out("Finished instantiate transaction with transaction id %s", transactionEvent.getTransactionID());

      try {
        successful.clear();
        failed.clear();

        client.setUserContext(sampleOrg.getUser(TESTUSER_1_NAME));

        ///////////////
        /// Send transaction proposal to all peers
        TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(chainCodeID);
        transactionProposalRequest.setFcn("invoke");
        transactionProposalRequest.setArgs(new String[]{"move", "a", "b", "100"});

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "TransactionProposalRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "TransactionProposalRequest".getBytes(UTF_8));
        tm2.put("result", ":)".getBytes(UTF_8));  /// This should be returned see chaincode.
        transactionProposalRequest.setTransientMap(tm2);

        out("sending transactionProposal to all peers with arguments: move(a,b,100)");

        Collection<ProposalResponse> transactionPropResp = chain.sendTransactionProposal(transactionProposalRequest, chain.getPeers());
        for (ProposalResponse response : transactionPropResp) {
          if (response.getStatus() == ProposalResponse.Status.SUCCESS) {
            out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
            successful.add(response);
          } else {
            failed.add(response);
          }
        }
        out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
          transactionPropResp.size(), successful.size(), failed.size());
        if (failed.size() > 0) {
          ProposalResponse firstTransactionProposalResponse = failed.iterator().next();

          throw new ServiceException("Not enough endorsers for invoke(move a,b,100):" + failed.size() + " endorser error: "
            + firstTransactionProposalResponse.getMessage()
            + ". Was verified: " + firstTransactionProposalResponse.isVerified());
        }
        out("Successfully received transaction proposal responses.");

        ProposalResponse resp = transactionPropResp.iterator().next();
        byte[] x = resp.getChainCodeActionResponsePayload(); // This is the data returned by the chaincode.
        String resultAsString = null;
        if (x != null) {
          resultAsString = new String(x, "UTF-8");
        }
//                    assertEquals(":)", resultAsString);
//
//                    assertEquals(200, resp.getChainCodeActionResponseStatus()); //Chaincode's status.
//
//                    TxReadWriteSetInfo readWriteSetInfo = resp.getChainCodeActionResponseReadWriteSetInfo();
//                    //See blockwaler below how to transverse this
//                    assertNotNull(readWriteSetInfo);
//                    assertTrue(readWriteSetInfo.getNsRwsetCount() > 0);
//
//                    ChainCodeID cid = resp.getChainCodeID();
//                    assertNotNull(cid);
//                    assertEquals(CHAIN_CODE_PATH, cid.getPath());
//                    assertEquals(CHAIN_CODE_NAME, cid.getName());
//                    assertEquals(CHAIN_CODE_VERSION, cid.getVersion());

        ////////////////////////////
        // Send Transaction Transaction to orderer
        out("Sending chain code transaction(move a,b,100) to orderer.");
        return chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);

      } catch (Exception e) {
        out("Caught an exception while invoking chaincode");
        e.printStackTrace();
        throw new ServiceException("Failed invoking chaincode with error : " + e.getMessage());
      }
    }).thenApply(transactionEvent -> {
      try {

        //waitOnFabric(0);
        //assertTrue(transactionEvent.isValid()); // must be valid to be here.
        out("Finished transaction with transaction id %s", transactionEvent.getTransactionID());
        testTxID = transactionEvent.getTransactionID(); // used in the channel queries later

        ////////////////////////////
        // Send Query Proposal to all peers
        //
        String expect = "" + (300 + delta);
        out("Now query chain code for the value of b.");
        QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
        queryByChaincodeRequest.setArgs(new String[]{"query", "b"});
        queryByChaincodeRequest.setFcn("invoke");
        queryByChaincodeRequest.setChaincodeID(chainCodeID);

        Map<String, byte[]> tm2 = new HashMap<>();
        tm2.put("HyperLedgerFabric", "QueryByChaincodeRequest:JavaSDK".getBytes(UTF_8));
        tm2.put("method", "QueryByChaincodeRequest".getBytes(UTF_8));
        queryByChaincodeRequest.setTransientMap(tm2);

        Collection<ProposalResponse> queryProposals = chain.queryByChaincode(queryByChaincodeRequest, chain.getPeers());
        for (ProposalResponse proposalResponse : queryProposals) {
          if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ProposalResponse.Status.SUCCESS) {
            ErrorInfo result = new ErrorInfo(500, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
              + ". Messages: " + proposalResponse.getMessage()
              + ". Was verified : " + proposalResponse.isVerified());
            return result;
          } else {
            String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
            out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
            //assertEquals(payload, expect);
          }
        }

        return null;
      } catch (Exception e) {
        out("Caught exception while running query");
        e.printStackTrace();
        throw new ServiceException("Failed during chaincode query with error : " + e.getMessage());
      }
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

  @RequestMapping(value = "/listchaincodes", method = RequestMethod.GET)
  public List<String> listchaincodes() throws InvalidArgumentException, ProposalException {
    List<String> result = new ArrayList<>();

    // Can only list if we are admin, normal users don't have the rights
    SampleOrg sampleOrg = TESTCONFIG.getIntegrationTestsSampleOrg("peerOrg1");
    client.setUserContext(sampleOrg.getPeerAdmin());

    Peer peer = getAPeer(chain);
    List<Query.ChaincodeInfo> ccinfoList = chain.queryInstantiatedChaincodes(peer);

    for (Query.ChaincodeInfo ccifo : ccinfoList) {
      result.add(ccifo.getName() + ":" + ccifo.getPath() + ":" + ccifo.getVersion());
    }

    return result;
  }

  /**
   * Just gets the first peer in the list of peers from a chain
   *
   * @param chain
   * @return
   */
  private Peer getAPeer(Chain chain) {
    final String chainName = chain.getName();
    LOGGER.debug("Getting a peer from chain %s", chainName);
    Collection<Peer> peers = chain.getPeers();
    LOGGER.debug("Found %d peers", peers.size());

    return peers.iterator().next();
  }

  @RequestMapping(value = "/chainqueries", method = RequestMethod.GET)
  public ErrorInfo chainqueries() throws ProposalException, InvalidArgumentException {

    SampleOrg sampleOrg = TESTCONFIG.getIntegrationTestsSampleOrg("peerOrg1");

    // We can only send channel queries to peers that are in the same org as the SDK user context
    // Get the peers from the current org being used and pick one randomly to send the queries to.
    Set<Peer> peerSet = sampleOrg.getPeers();
    Peer queryPeer = peerSet.iterator().next();
    out("Using peer %s for channel queries", queryPeer.getName());

    BlockchainInfo channelInfo = chain.queryBlockchainInfo(queryPeer);
    out("Channel info for : " + FOO_CHAIN_NAME);
    out("Channel height: " + channelInfo.getHeight());
    String chainCurrentHash = Hex.encodeHexString(channelInfo.getCurrentBlockHash());
    String chainPreviousHash = Hex.encodeHexString(channelInfo.getPreviousBlockHash());
    out("Channel current block hash: " + chainCurrentHash);
    out("Channel previous block hash: " + chainPreviousHash);

    // Query by block number. Should return latest block, i.e. block number 2
    BlockInfo returnedBlock = chain.queryBlockByNumber(queryPeer, channelInfo.getHeight() - 1);
    String previousHash = Hex.encodeHexString(returnedBlock.getPreviousHash());
    out("queryBlockByNumber returned correct block with blockNumber " + returnedBlock.getBlockNumber()
      + " \n previous_hash " + previousHash);
    //assertEquals(channelInfo.getHeight() - 1, returnedBlock.getBlockNumber());
    //assertEquals(chainPreviousHash, previousHash);

    // Query by block hash. Using latest block's previous hash so should return block number 1
    byte[] hashQuery = returnedBlock.getPreviousHash();
    returnedBlock = chain.queryBlockByHash(queryPeer, hashQuery);
    out("queryBlockByHash returned block with blockNumber " + returnedBlock.getBlockNumber());
    //assertEquals(channelInfo.getHeight() - 2, returnedBlock.getBlockNumber());

    // Query block by TxID. Since it's the last TxID, should be block 2
    returnedBlock = chain.queryBlockByTransactionID(queryPeer, testTxID);
    out("queryBlockByTxID returned block with blockNumber " + returnedBlock.getBlockNumber());
    //assertEquals(channelInfo.getHeight() - 1, returnedBlock.getBlockNumber());

    // query transaction by ID
    TransactionInfo txInfo = chain.queryTransactionByID(queryPeer, testTxID);
    out("QueryTransactionByID returned TransactionInfo: txID " + txInfo.getTransactionID()
      + "\n     validation code " + txInfo.getValidationCode().getNumber());

    ErrorInfo result = new ErrorInfo(0, "", "OK");
    return result;
  }

  @RequestMapping(value = "/querya", method = RequestMethod.GET)
  public ErrorInfo querya() throws ProposalException, InvalidArgumentException {

    ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
      .setVersion(CHAIN_CODE_VERSION)
      .setPath(CHAIN_CODE_PATH).build();

    out("Now query chaincode on chain %s for the value of a", chain.getName());
    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
    queryByChaincodeRequest.setArgs(new String[]{"query", "a"});
    queryByChaincodeRequest.setFcn("invoke");
    queryByChaincodeRequest.setChaincodeID(chainCodeID);

    Collection<ProposalResponse> queryProposals;

    try {
      queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
    } catch (InvalidArgumentException | ProposalException e) {
      throw new CompletionException(e);
    }

    for (ProposalResponse proposalResponse : queryProposals) {
      if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
        ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
          + ". Messages: " + proposalResponse.getMessage()
          + ". Was verified : " + proposalResponse.isVerified());
        return result;
      } else {
        String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
        out("Query payload of a from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
      }
    }

    ErrorInfo result = new ErrorInfo(0, "", "OK");
    return result;
  }

  @RequestMapping(value = "/queryb", method = RequestMethod.GET)
  public ErrorInfo queryb() throws ProposalException, InvalidArgumentException {

    ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
      .setVersion(CHAIN_CODE_VERSION)
      .setPath(CHAIN_CODE_PATH).build();

    out("Now query chaincode on chain %s for the value of b", chain.getName());
    QueryByChaincodeRequest queryByChaincodeRequest = client.newQueryProposalRequest();
    queryByChaincodeRequest.setArgs(new String[]{"query", "b"});
    queryByChaincodeRequest.setFcn("invoke");
    queryByChaincodeRequest.setChaincodeID(chainCodeID);

    Collection<ProposalResponse> queryProposals;

    try {
      queryProposals = chain.queryByChaincode(queryByChaincodeRequest);
    } catch (InvalidArgumentException | ProposalException e) {
      throw new CompletionException(e);
    }

    for (ProposalResponse proposalResponse : queryProposals) {
      if (!proposalResponse.isVerified() || proposalResponse.getStatus() != ChainCodeResponse.Status.SUCCESS) {
        ErrorInfo result = new ErrorInfo(0, "", "Failed query proposal from peer " + proposalResponse.getPeer().getName() + " status: " + proposalResponse.getStatus()
          + ". Messages: " + proposalResponse.getMessage()
          + ". Was verified : " + proposalResponse.isVerified());
        return result;
      } else {
        String payload = proposalResponse.getProposalResponse().getResponse().getPayload().toStringUtf8();
        out("Query payload of b from peer %s returned %s", proposalResponse.getPeer().getName(), payload);
      }
    }

    ErrorInfo result = new ErrorInfo(0, "", "OK");
    return result;
  }

  @RequestMapping(value = "/move1fromatob", method = RequestMethod.GET)
  public ErrorInfo move1fromatob() throws ProposalException, InvalidArgumentException {

    ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
      .setVersion(CHAIN_CODE_VERSION)
      .setPath(CHAIN_CODE_PATH).build();

    try {
      Collection<ProposalResponse> successful = new LinkedList<>();
      Collection<ProposalResponse> failed = new LinkedList<>();

      ///////////////
      /// Send transaction proposal to all peers
      TransactionProposalRequest transactionProposalRequest = client.newTransactionProposalRequest();
      transactionProposalRequest.setChaincodeID(chainCodeID);
      transactionProposalRequest.setFcn("invoke");
      transactionProposalRequest.setArgs(new String[]{"move", "a", "b", "1"});
      out("sending transaction proposal to all peers with arguments: move(a,b,%s)", "1");

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
        ProposalResponse firstTransactionProposalResponse = failed.iterator().next();

        throw new ProposalException(format("Not enough endorsers for invoke(move a,b,%s):%d endorser error:%s. Was verified:%b",
          "1", firstTransactionProposalResponse.getStatus().getStatus(), firstTransactionProposalResponse.getMessage(), firstTransactionProposalResponse.isVerified()));

      }
      out("Successfully received transaction proposal responses.");

      ////////////////////////////
      // Send transaction to orderer
      out("Sending chain code transaction(move a,b,%s) to orderer.", "1");
      chain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
    } catch (Exception e) {

      ErrorInfo result = new ErrorInfo(500, "", "CompletionException " + e.getMessage());
      return result;
    }

    ErrorInfo result = new ErrorInfo(0, "", "OK");
    return result;
  }

  @RequestMapping(value = "/dumpchain", method = RequestMethod.GET)
  public ErrorInfo dumpchain() throws InvalidProtocolBufferException, InvalidArgumentException, ProposalException, UnsupportedEncodingException {
    out("\nTraverse the blocks for chain %s ", chain.getName());
    blockWalker(chain);

    ErrorInfo result = new ErrorInfo(0, "", "OK");
    return result;
  }

  @RequestMapping(value = "/destroychain", method = RequestMethod.GET)
  public ErrorInfo destroychain() {

    if (chain != null) {
      chain.shutdown(true);
    } else {
      ErrorInfo result = new ErrorInfo(500, "", "Chain was not initialised");
      return result;
    }

    ErrorInfo result = new ErrorInfo(0, "", "OK");
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

  static void out(String format, Object... args) {

    System.err.flush();
    System.out.flush();

    System.out.println(format(format, args));
    System.err.flush();
    System.out.flush();

  }

  void blockWalker(Chain chain) throws InvalidProtocolBufferException, InvalidArgumentException, ProposalException, UnsupportedEncodingException {

    try {
      BlockchainInfo channelInfo = chain.queryBlockchainInfo();

      for (long current = channelInfo.getHeight() - 1; current > -1; --current) {
        BlockInfo returnedBlock = chain.queryBlockByNumber(current);
        final long blockNumber = returnedBlock.getBlockNumber();

        out("current block number %d has data hash: %s", blockNumber, Hex.encodeHexString(returnedBlock.getDataHash()));
        out("current block number %d has previous hash id: %s", blockNumber, Hex.encodeHexString(returnedBlock.getPreviousHash()));

        final int envelopCount = returnedBlock.getEnvelopCount();
        //assertEquals(1, envelopCount);
        out("current block number %d has %d envelope count:", blockNumber, returnedBlock.getEnvelopCount());
        int i = 0;
        for (BlockInfo.EnvelopeInfo envelopeInfo : returnedBlock.getEnvelopeInfos()) {
          ++i;

          out("  Transaction number %d has transaction id: %s", i, envelopeInfo.getTransactionID());
          final String channelId = envelopeInfo.getChannelId();
          //assertTrue("foo".equals(channelId) || "bar".equals(channelId));

          out("  Transaction number %d has channel id: %s", i, channelId);
          out("  Transaction number %d has epoch: %d", i, envelopeInfo.getEpoch());
          out("  Transaction number %d has transaction timestamp: %tB %<te,  %<tY  %<tT %<Tp", i, envelopeInfo.getTimestamp());
          out("  Transaction number %d has type id: %s", i, "" + envelopeInfo.getType());

          if (envelopeInfo.getType() == TRANSACTION_ENVELOPE) {
            BlockInfo.TansactionEnvelopeInfo tansactionEnvelopeInfo = (BlockInfo.TansactionEnvelopeInfo) envelopeInfo;

            out("  Transaction number %d has %d actions", i, tansactionEnvelopeInfo.getTransactionActionInfoCount());
            //assertEquals(1, tansactionEnvelopeInfo.getTransactionActionInfoCount()); // for now there is only 1 action per transaction.
            out("  Transaction number %d isValid %b", i, tansactionEnvelopeInfo.isValid());
            //assertEquals(tansactionEnvelopeInfo.isValid(), true);
            out("  Transaction number %d validation code %d", i, tansactionEnvelopeInfo.getValidationCode());
            //assertEquals(0, tansactionEnvelopeInfo.getValidationCode());

            int j = 0;
            for (BlockInfo.TansactionEnvelopeInfo.TransactionActionInfo transactionActionInfo : tansactionEnvelopeInfo.getTransactionActionInfos()) {
              ++j;
              out("   Transaction action %d has response status %d", j, transactionActionInfo.getResponseStatus());
              //assertEquals(200, transactionActionInfo.getResponseStatus());
              out("   Transaction action %d has response message bytes as string: %s", j,
                printableString(new String(transactionActionInfo.getResponseMessageBytes(), "UTF-8")));

              out("   Transaction action %d has %d endorsements", j, transactionActionInfo.getEndorsementsCount());
              //assertEquals(2, transactionActionInfo.getEndorsementsCount());
              for (int n = 0; n < transactionActionInfo.getEndorsementsCount(); ++n) {
                BlockInfo.EndorserInfo endorserInfo = transactionActionInfo.getEndorsementInfo(n);
                out("Endorser %d signature: %s", n, Hex.encodeHexString(endorserInfo.getSignature()));
                out("Endorser %d endorser: %s", n, new String(endorserInfo.getEndorser(), "UTF-8"));
              }
              out("   Transaction action %d has %d chain code input arguments", j, transactionActionInfo.getChaincodeInputArgsCount());
              for (int z = 0; z < transactionActionInfo.getChaincodeInputArgsCount(); ++z) {

                out("     Transaction action %d has chaincode input argument %d is: %s", j, z,
                  printableString(new String(transactionActionInfo.getChaincodeInputArgs(z), "UTF-8")));
              }

              out("   Transaction action %d proposal response status: %d", j,
                transactionActionInfo.getProposalResponseStatus());

              out("   Transaction action %d proposal response payload: %s", j,
                printableString(new String(transactionActionInfo.getProposalResponsePayload())));

              TxReadWriteSetInfo rwsetInfo = transactionActionInfo.getTxReadWriteSet();
              if (null != rwsetInfo) {

                out("   Transaction action %d has %d name space read write sets", j, rwsetInfo.getNsRwsetCount());

                for (TxReadWriteSetInfo.NsRwsetInfo nsRwsetInfo : rwsetInfo.getNsRwsetInfos()) {

                  final String namespace = nsRwsetInfo.getNaamespace();
                  KvRwset.KVRWSet rws = nsRwsetInfo.getRwset();

                  int rs = -1;
                  for (KvRwset.KVRead readList : rws.getReadsList()) {
                    rs++;

                    out("     Namespace %s read set %d key %s  version [%d:%d]", namespace, rs, readList.getKey(),
                      readList.getVersion().getBlockNum(), readList.getVersion().getTxNum());

//                                        if ("bar".equals(channelId) && blockNumber == 2) {
//
//                                            if ("example_cc_go".equals(namespace)) {
////                                                if (rs == 0) {
////                                                    assertEquals("a", readList.getKey());
////                                                    assertEquals(1, readList.getVersion().getBlockNum());
////                                                    assertEquals(0, readList.getVersion().getTxNum());
////                                                } else if (rs == 1) {
////                                                    assertEquals("b", readList.getKey());
////                                                    assertEquals(1, readList.getVersion().getBlockNum());
////                                                    assertEquals(0, readList.getVersion().getTxNum());
////                                                } else {
////                                                    fail(format("unexpected readset %d", rs));
////                                                }
//
//                                                txExpected.remove("readset1");
//                                            }
//                                        }
                  }

                  rs = -1;

                  for (KvRwset.KVWrite writeList : rws.getWritesList()) {
                    rs++;

                    String valAsString = printableString(new String(writeList.getValue().toByteArray(), "UTF-8"));

                    out("     Namespace %s write set %d key %s has value '%s' ", namespace, rs,
                      writeList.getKey(),
                      valAsString);

                    if ("bar".equals(channelId) && blockNumber == 2) {
                      if (rs == 0) {
                        //assertEquals("a", writeList.getKey());

                        //assertEquals("400", valAsString);
                      } else if (rs == 1) {
                        //assertEquals("b", writeList.getKey());

                        //assertEquals("400", valAsString);
                      } else {
                        //fail(format("unexpected writeset %d", rs));
                      }
                    }

                  }
                }

              }

            }

          }

        }

      }
    } catch (InvalidProtocolBufferRuntimeException e) {
      throw e.getCause();
    }
  }

  static String printableString(final String string) {
    int maxLogStringLength = 64;
    if (string == null || string.length() == 0) {
      return string;
    }

    String ret = string.replaceAll("[^\\p{Print}]", "?");

    ret = ret.substring(0, Math.min(ret.length(), maxLogStringLength)) + (ret.length() > maxLogStringLength ? "..." : "");

    return ret;

  }

}
