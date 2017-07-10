package ch.uzh.fabric.controller;

import ch.uzh.fabric.FabricApplication;
import ch.uzh.fabric.config.ErrorInfo;
import ch.uzh.fabric.config.TestConfig;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

@Controller
public class AppController {

	private static final String CHAIN_CODE_NAME = "car_cc_go";
	private static final String CHAIN_CODE_PATH = "github.com/car_cc";
	private static final String CHAIN_CODE_VERSION = "1";

	private static final String TEST_USER = "test_user1";
	private static final String TEST_ROLE = "garage";
	private static final String TEST_VIN = "WVW ZZZ 6RZ HY26 0780";

	private static final TestConfig TESTCONFIG = TestConfig.getConfig();

	@Autowired
	private HFClient hfClient;
	@Autowired
	private Chain blockChain;


	@RequestMapping("/")
	public String root() {
		return "redirect:/login";
	}

	@RequestMapping("/index")
	public String index() {
		System.out.println(blockChain.toString());
		return "index";
	}

	@RequestMapping("/car/create")
	public String createCar() {
		ChainCodeID chainCodeID = ChainCodeID.newBuilder().setName(CHAIN_CODE_NAME)
				.setVersion(CHAIN_CODE_VERSION)
				.setPath(CHAIN_CODE_PATH).build();

		try {
			Collection<ProposalResponse> successful = new LinkedList<>();
			Collection<ProposalResponse> failed = new LinkedList<>();

			TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
			transactionProposalRequest.setChaincodeID(chainCodeID);
			transactionProposalRequest.setFcn("create");

			transactionProposalRequest.setArgs(new String[]{TEST_USER, TEST_ROLE, "{ \"vin\": \"" + TEST_VIN + "\" }", ""});
			FabricApplication.out("sending transaction proposal to 'create' a car to all peers");

			Collection<ProposalResponse> invokePropResp = blockChain.sendTransactionProposal(transactionProposalRequest, blockChain.getPeers());
			for (ProposalResponse response : invokePropResp) {
				if (response.getStatus() == ChainCodeResponse.Status.SUCCESS) {
					FabricApplication.out("Successful transaction proposal response Txid: %s from peer %s", response.getTransactionID(), response.getPeer().getName());
					successful.add(response);
				} else {
					failed.add(response);
				}
			}
			FabricApplication.out("Received %d transaction proposal responses. Successful+verified: %d . Failed: %d",
					invokePropResp.size(), successful.size(), failed.size());
			if (failed.size() > 0) {
				throw new ProposalException("Not enough endorsers for invoke");

			}
			FabricApplication.out("Successfully received transaction proposal responses.");

			FabricApplication.out("Sending chain code transaction to orderer");
			blockChain.sendTransaction(successful).get(TESTCONFIG.getTransactionWaitTime(), TimeUnit.SECONDS);
		} catch (Exception e) {
			FabricApplication.out(e.toString());
			e.printStackTrace();
			ErrorInfo result = new ErrorInfo(500, "", "CompletionException " + e.getMessage());
			//return result;
			return "user/index";
		}

		ErrorInfo result = new ErrorInfo(0, "", "OK");
		//return result;
		return "user/index";
	}

	@RequestMapping("/user/index")
	public String userIndex() {
		return "user/index";
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

	@RequestMapping("/chain")
	public String chain(){
		return blockChain.toString();
	}


}
