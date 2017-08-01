package ch.uzh.fabric.controller;

import ch.uzh.fabric.config.SecurityConfig;
import ch.uzh.fabric.model.Car;
import ch.uzh.fabric.model.ProposalAndCar;
import ch.uzh.fabric.model.ProposalData;
import ch.uzh.fabric.service.CarService;
import ch.uzh.fabric.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Controller
@RequestMapping("/dot")
public class DotController {
    @Autowired
    private UserService userService;
    @Autowired
    private CarService carService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String registration(Model model,
                               RedirectAttributes redirAttr,
                               Authentication auth,
                               @RequestParam(required = false) String success,
                               @RequestParam(required = false) String error) {
        String username = (model != null) ? auth.getName() : SecurityConfig.BOOTSTRAP_DOT_USER;
        String role = (model != null) ? userService.getRole(auth) : SecurityConfig.BOOTSTRAP_DOT_ROLE;

        Collection<ProposalData> registrationProposals;
        Collection<ProposalAndCar> proposalsAndCars = new ArrayList<>();

        try {
            registrationProposals = carService.getRegistrationProposals(username, role);
            for (ProposalData proposal : registrationProposals) {
                Car car = carService.getCar(proposal.getUsername(), role, proposal.getCar());
                proposalsAndCars.add(new ProposalAndCar(proposal, car));
            }
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("proposalAndCarData", proposalsAndCars);
        model.addAttribute("role", role.toUpperCase());

        return "dot/registration";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String registrationAccept(RedirectAttributes redirAttr,
                                     Authentication auth,
                                     @RequestParam String vin,
                                     @RequestParam String owner) {
        String role = (redirAttr != null) ? userService.getRole(auth) : SecurityConfig.BOOTSTRAP_DOT_ROLE;

        try {
            carService.register(owner, role, vin);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/dot/";
        }

        if (redirAttr != null) {
            redirAttr.addAttribute("success", "Registration proposal accepted. Car with Vin: '" + vin + "' is successfully registered.");
        }

        return "redirect:/dot/";
    }

    @RequestMapping(value = "/confirmation", method = RequestMethod.GET)
    public String confirm(Model model,
                          RedirectAttributes redirAttr,
                          Authentication auth,
                          @RequestParam(required = false) String success,
                          @RequestParam(required = false) String error) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        Collection<Car> carsToConfirm = new ArrayList<>();

        try {
            carsToConfirm = carService.getCarsToConfirm(username, role);
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("cars", carsToConfirm);
        model.addAttribute("role", role.toUpperCase());

        return "dot/confirmation";
    }

    @RequestMapping(value = "/confirmation", method = RequestMethod.POST)
    public String confirmationAccept(RedirectAttributes redirAttr,
                                     Authentication auth,
                                     @RequestParam String vin,
                                     @RequestParam String numberplate) {
        String username = (redirAttr != null) ? auth.getName() : SecurityConfig.BOOTSTRAP_DOT_USER;
        String role = (redirAttr != null) ? userService.getRole(auth) : SecurityConfig.BOOTSTRAP_DOT_ROLE;

        try {
            carService.confirm(username, role, vin, numberplate);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/dot/confirmation";
        }

        redirAttr.addAttribute("success", "Confirmation for car '" + vin + "' with numberplate '" + numberplate + "' successful.");
        return "redirect:/dot/confirmation";
    }

    @RequestMapping(value = "/revocation", method = RequestMethod.GET)
    public String revocation(Model model,
                             RedirectAttributes redirAttr,
                             Authentication auth,
                             @RequestParam(required = false) String error,
                             @RequestParam(required = false) String success) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        Collection<Car> carList = new ArrayList<>();

        try {
            Map<String, String> revocationProposals = carService.getRevocationProposals(username, role);
            for (Map.Entry<String, String> e : revocationProposals.entrySet()) {
                Car car = carService.getCar(e.getValue(), role, e.getKey());
                carList.add(car);
            }
        } catch (Exception e) {
            error = e.getMessage();
        }

        model.addAttribute("cars", carList);
        model.addAttribute("error", error);
        model.addAttribute("success", success);
        model.addAttribute("role", role.toUpperCase());
        return "dot/revocation";
    }

    @RequestMapping(value = "/revocation", method = RequestMethod.POST)
    public String revocationAccept(RedirectAttributes redirAttr, Authentication auth, @RequestParam String vin, @RequestParam String owner) {
        String role = userService.getRole(auth);

        try {
            carService.revoke(owner, role, vin);
        } catch (Exception e) {
            redirAttr.addAttribute("error", e.getMessage());
            return "redirect:/dot/revocation";
        }

        redirAttr.addAttribute("success", "Successfully revoked car with VIN '" + vin + "' of user '" + owner + "'");
        return "redirect:/dot/revocation";
    }

    @RequestMapping("/all-cars")
    public String allCars(RedirectAttributes redirAttr, Authentication auth, Model model) {
        String username = auth.getName();
        String role = userService.getRole(auth);
        Collection<Car> cars = new ArrayList<>();

        try {
            cars = carService.getAllCars(username, role);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        model.addAttribute("cars", cars);
        model.addAttribute("role", role.toUpperCase());
        return "dot/all-cars";
    }

}
