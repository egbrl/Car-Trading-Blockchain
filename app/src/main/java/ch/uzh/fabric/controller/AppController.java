package ch.uzh.fabric.controller;

import ch.uzh.fabric.config.ProfileProperties;
import ch.uzh.fabric.config.SecurityConfig;
import ch.uzh.fabric.model.*;
import ch.uzh.fabric.service.CarService;
import ch.uzh.fabric.service.UserService;
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
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
public class AppController {
    public static final SimpleDateFormat timeFormat = new SimpleDateFormat("dd.MM.yyyy, HH:mm");

    @Autowired
    private UserService userService;
    @Autowired
    private CarService carService;

    @PostConstruct
    public void AppController() {
        timeFormat.setTimeZone(TimeZone.getTimeZone("Europe/Zurich"));
    }

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
    public String account(Authentication auth, Model model) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        ProfileProperties.User user = userService.findOrCreateUser(username, role);

        model.addAttribute("orgName", user.getOrganization());
        model.addAttribute("role", role.toUpperCase());
        return "account";
    }

    @RequestMapping(value = "/account", method = RequestMethod.POST)
    public String accountChange(Authentication auth, Model model, @RequestParam("orgName") String orgName) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        ProfileProperties.User user = userService.findOrCreateUser(username, role);
        user.setOrganization(orgName);

        model.addAttribute("orgName", user.getOrganization());
        model.addAttribute("role", role.toUpperCase());
        return "account";
    }

    @RequestMapping("/index")
    public String index(Authentication auth,
                        Model model,
                        RedirectAttributes redirAttr,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String success) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        if (role.equals("dot")) {
            return "redirect:/dot/";
        } else if (role.equals("insurer")) {
            return "redirect:/insurance/";
        }

        Collection<Car> cars = new ArrayList<>();

        try {
            cars = carService.getCars(username, role);
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("cars", cars);
        model.addAttribute("role", role.toUpperCase());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "index";
    }

    @RequestMapping(value = "/import", method = RequestMethod.GET)
    public String importCar(Model model,
                            Authentication auth,
                            @RequestParam(required = false) String error,
                            @RequestParam(required = false) String success,
                            @ModelAttribute Car car,
                            @ModelAttribute ProposalData proposalData) {
        String role = userService.getRole(auth);
        model.addAttribute("role", role.toUpperCase());
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        return "import";
    }

    @RequestMapping(value = "/import", method = RequestMethod.POST)
    public String importCar(Model model,
                            RedirectAttributes redirAttr,
                            Authentication auth,
                            @ModelAttribute Car car,
                            @ModelAttribute ProposalData proposalData) {
        String username = (model != null) ? auth.getName() : SecurityConfig.BOOTSTRAP_GARAGE_USER;
        String role = (model != null) ? userService.getRole(auth) : SecurityConfig.BOOTSTRAP_GARAGE_ROLE;

        proposalData.setCar(car.getVin());
        try {
            carService.importCar(username, role, car, proposalData);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/import";
        }

        if (model != null && redirAttr != null) {
            model.addAttribute("role", role.toUpperCase());
            redirAttr.addAttribute("success", "Successfully imported car with VIN '" + car.getVin() + "'");
        }

        return "redirect:/index";
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

    @RequestMapping("/revocationProposal")
    public String revocationProposal(RedirectAttributes redirAttr, Authentication auth, @RequestParam String vin) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        try {
            carService.revocationProposal(username, role, vin);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/index";
        }

        redirAttr.addAttribute("success", "Request for revocation registered, DOT is notified.");
        return "redirect:/index";
    }

    @RequestMapping(value = "/insure", method = RequestMethod.GET)
    public String insure(Model model,
                         RedirectAttributes redirAttr,
                         Authentication auth,
                         @RequestParam(required = false) String success,
                         @RequestParam(required = false) String error,
                         @RequestParam(required = false) String activeVin) {
        String username = auth.getName();
        String role = userService.getRole(auth);
        Collection<Car> cars = new ArrayList<>();

        try {
            cars = carService.getCars(username, role);
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("activeVin", activeVin);
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        model.addAttribute("cars", cars);
        model.addAttribute("role", role.toUpperCase());
        return "insure";
    }

    @RequestMapping(value = "/insure", method = RequestMethod.POST)
    public String insure(RedirectAttributes redirAttr, Authentication auth, @RequestParam String vin, @RequestParam String company) {
        String username = (redirAttr != null) ? auth.getName() : SecurityConfig.BOOTSTRAP_GARAGE_USER;
        String role = (redirAttr != null) ? userService.getRole(auth) : SecurityConfig.BOOTSTRAP_GARAGE_ROLE;

        try {
            carService.insureProposal(username, role, vin, company);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/insure";
        }

        if (redirAttr != null) {
            redirAttr.addAttribute("success", "Insurance proposal saved. '" + company + "' will get back to you for confirmation.");
        }

        return "redirect:/insure";
    }

    @RequestMapping(value = "/sell", method = RequestMethod.GET)
    public String sell(Model model,
                       RedirectAttributes redirAttr,
                       Authentication auth,
                       @RequestParam(required = false) String success,
                       @RequestParam(required = false) String error,
                       @RequestParam(required = false) String activeVin) {
        String username = auth.getName();
        String role = userService.getRole(auth);
        Collection<Car> cars = new ArrayList<>();

        try {
            cars = carService.getCars(username, role);
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("activeVin", activeVin);
        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("cars", cars);
        model.addAttribute("role", role.toUpperCase());
        return "sell";
    }

    @RequestMapping(value = "/sell", method = RequestMethod.POST)
    public String sellOfferCreate(RedirectAttributes redirAttr,
                                  Authentication auth,
                                  @RequestParam String vin,
                                  @RequestParam(required = false) String buyer,
                                  @RequestParam(required = false) String price) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        try {
            carService.createSellingOffer(username, role, price, vin, buyer);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/sell";
        }

        redirAttr.addAttribute("success", "Successfully sent selling offer for car '" + vin + "' to user '" + buyer + "'");
        return "redirect:/sell";
    }

    @RequestMapping(value = "/offers", method = RequestMethod.GET)
    public String sellOfferShow(Model model,
                                Authentication auth,
                                @RequestParam(required = false) String success,
                                @RequestParam(required = false) String error) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        Collection<Offer> offers;
        Collection<OfferAndCar> offersAndCars = new ArrayList<>();

        try {
            offers = carService.getSalesOffers(username, role);
            for (Offer offer : offers) {
                Car car = carService.getCar(offer.getSeller(), role, offer.getVin());
                offersAndCars.add(new OfferAndCar(offer, car));
            }
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("offersAndCars", offersAndCars);
        model.addAttribute("role", role.toUpperCase());
        return "offers";
    }


    @RequestMapping(value = "/offers", method = RequestMethod.POST)
    public String sellOfferAccept(RedirectAttributes redirAttr,
                                  Authentication auth,
                                  @RequestParam String vin,
                                  @RequestParam String seller,
                                  @RequestParam String price) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        try {
            carService.sell(seller, role, vin, username);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/offers";
        }

        redirAttr.addAttribute("success", "Successfully bought car '" + vin + "' from user '" + seller + "' at price '" + price + "'.");
        return "redirect:/offers";
    }

    @RequestMapping(value = "/history", method = RequestMethod.GET)
    public String history(Model model,
                          RedirectAttributes redirAttr,
                          Authentication auth,
                          @RequestParam String vin,
                          @RequestParam(required = false) String error) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        Map<Integer, Car> history = new HashMap<>();
        try {
            history = carService.getCarHistory(username, role, vin);
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("error", error);
        model.addAttribute("vin", vin);
        model.addAttribute("history", history);
        model.addAttribute("timeFmt", timeFormat);
        model.addAttribute("role", role.toUpperCase());

        return "history";
    }
}
