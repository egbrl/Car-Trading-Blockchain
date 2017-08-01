package ch.uzh.fabric.config;

import ch.uzh.fabric.model.Car;
import ch.uzh.fabric.model.Certificate;
import ch.uzh.fabric.model.ProposalData;
import ch.uzh.fabric.service.CarService;
import ch.uzh.fabric.service.DotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import static ch.uzh.fabric.config.SecurityConfig.*;

@Component
public class Bootstrap implements ApplicationRunner {

    private static final String TEST_INSURANCE_COMPANY = "AXA";
    private static final String TEST_VIN = "WVWZZZ6RZHY260780";
    private static final String TEST_VIN2 = "XYZDZZ6RZHY820780";
    private static final String TEST_VIN3 = "WVWQAW6RZHY140783";
    private static final String TEST_VIN4 = "AVCQA8WJZHY140783";

    @Autowired
    private CarService carService;
    @Autowired
    private DotService dotService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // potential buyer
        carService.createUser(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_GARAGE_ROLE, BOOTSTRAP_PRIVATE_USER);
        carService.createUser(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_GARAGE_ROLE, BOOTSTRAP_GARAGE_USER);

        // fully confirmed car
        // sales offer already made for user
        carService.importCar(BOOTSTRAP_GARAGE_USER,
                BOOTSTRAP_GARAGE_ROLE,
                new Car(
                        new Certificate(
                                null,
                                null,
                                null,
                                null,
                                "white",
                                "C350",
                                "Mercedes"), 0, TEST_VIN),
                new ProposalData(
                        "4+1",
                        4,
                        2,
                        200));
        dotService.register(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_DOT_ROLE, TEST_VIN);
        carService.insureProposal(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_GARAGE_ROLE, TEST_VIN, TEST_INSURANCE_COMPANY);
        carService.acceptInsurance(BOOTSTRAP_INSURANCE_USER, BOOTSTRAP_INSURANCE_ROLE, BOOTSTRAP_GARAGE_USER, TEST_VIN, TEST_INSURANCE_COMPANY);
//        carService.confirm(BOOTSTRAP_DOT_USER, BOOTSTRAP_DOT_ROLE, TEST_VIN, "ZH 99837");
        carService.createSellingOffer(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_GARAGE_ROLE, "5", TEST_VIN, BOOTSTRAP_PRIVATE_USER);
//        dotService.revoke(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_DOT_ROLE, TEST_VIN);
//
//        // create an unregistered car
//        // with insurance proposal
//        carService.importCar(BOOTSTRAP_GARAGE_USER,
//                BOOTSTRAP_GARAGE_ROLE,
//                new Car(
//                        new Certificate(
//                                null,
//                                null,
//                                null,
//                                null,
//                                "blue",
//                                "A8",
//                                "Audi"), 0, TEST_VIN2),
//                new ProposalData(
//                        "5",
//                        8,
//                        2,
//                        200));
//        carService.insureProposal(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_GARAGE_ROLE, TEST_VIN2, TEST_INSURANCE_COMPANY);
//
//        // create a registered car
//        // without insurance
//        carService.importCar(BOOTSTRAP_GARAGE_USER,
//                BOOTSTRAP_GARAGE_ROLE,
//                new Car(
//                        new Certificate(
//                                null,
//                                null,
//                                null,
//                                null,
//                                "red",
//                                "TDI",
//                                "VW"), 0, TEST_VIN3),
//                new ProposalData(
//                        "5",
//                        4,
//                        2,
//                        150));
//        dotService.register(BOOTSTRAP_GARAGE_USER, BOOTSTRAP_DOT_ROLE, TEST_VIN3);
//
//        // create a registered and insured batmobile
//        // ready to be confirmed
//        carService.importCar(BOOTSTRAP_PRIVATE_USER,
//                BOOTSTRAP_PRIVATE_USER_ROLE,
//                new Car(
//                        new Certificate(
//                                null,
//                                null,
//                                null,
//                                null,
//                                "black",
//                                "Batmobile",
//                                "Wayne Enterprises"), 0, TEST_VIN4),
//                new ProposalData(
//                        "5",
//                        8,
//                        2,
//                        250));
//        dotService.register(BOOTSTRAP_PRIVATE_USER, BOOTSTRAP_DOT_ROLE, TEST_VIN4);
//        carService.insureProposal(BOOTSTRAP_PRIVATE_USER, BOOTSTRAP_PRIVATE_USER_ROLE, TEST_VIN4, TEST_INSURANCE_COMPANY);
//        carService.acceptInsurance(BOOTSTRAP_INSURANCE_USER, BOOTSTRAP_INSURANCE_ROLE, BOOTSTRAP_PRIVATE_USER, TEST_VIN4, TEST_INSURANCE_COMPANY);

        System.out.println(
                "  _   _                       _          _                                      _         \n" +
                " | | | |_   _ _ __   ___ _ __| | ___  __| | __ _  ___ _ __   _ __ ___  __ _  __| |_   _   \n" +
                " | |_| | | | | '_ \\ / _ \\ '__| |/ _ \\/ _` |/ _` |/ _ \\ '__| | '__/ _ \\/ _` |/ _` | | | |  \n" +
                " |  _  | |_| | |_) |  __/ |  | |  __/ (_| | (_| |  __/ |    | | |  __/ (_| | (_| | |_| |_ \n" +
                " |_| |_|\\__, | .__/ \\___|_|  |_|\\___|\\__,_|\\__, |\\___|_|    |_|  \\___|\\__,_|\\__,_|\\__, ( )\n" +
                "  _     |___/|_|                      _    |___/     _             _              |___/|/ \n" +
                " | |__   __ _ _ __  _ __  _   _    __| |_ __(_)_   _(_)_ __   __ _| |                     \n" +
                " | '_ \\ / _` | '_ \\| '_ \\| | | |  / _` | '__| \\ \\ / / | '_ \\ / _` | |                     \n" +
                " | | | | (_| | |_) | |_) | |_| | | (_| | |  | |\\ V /| | | | | (_| |_|                     \n" +
                " |_| |_|\\__,_| .__/| .__/ \\__, |  \\__,_|_|  |_| \\_/ |_|_| |_|\\__, (_)                     \n" +
                "             |_|   |_|    |___/                              |___/                        ");
        System.out.println(
                "       _______\n" +
                "       //  ||\\ \\\n" +
                " _____//___||_\\ \\___\n" +
                " )  _          _    \\\n" +
                " |_/ \\________/ \\___|\n" +
                "___\\_/________\\_/______");
    }
}
