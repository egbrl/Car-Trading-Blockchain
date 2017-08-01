package ch.uzh.fabric.controller;

import ch.uzh.fabric.config.ProfileProperties;
import ch.uzh.fabric.config.SecurityConfig;
import ch.uzh.fabric.model.Car;
import ch.uzh.fabric.model.InsPropAndCar;
import ch.uzh.fabric.model.InsureProposal;
import ch.uzh.fabric.model.Insurer;
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

@Controller
@RequestMapping("/insurance")
public class InsuranceController {
    @Autowired
    private UserService userService;
    @Autowired
    private CarService carService;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String insurance(Model model,
                            Authentication auth,
                            @RequestParam(required = false) String success,
                            @RequestParam(required = false) String error) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        ProfileProperties.User user = userService.findOrCreateUser(username, role);
        String companyName = user.getOrganization();
        Insurer insurer;

        Collection<InsPropAndCar> insPropsAndCars = new ArrayList<>();

        try {
            insurer = carService.getInsurer(username, role, companyName);
            for (InsureProposal proposal : insurer.getProposals()) {
                Car car = carService.getCar(proposal.getUser(), "user", proposal.getCar());
                proposal.setRegistered(car.isRegistered());
                insPropsAndCars.add(new InsPropAndCar(proposal, car));
            }
        } catch (Exception e) {
            insurer = new Insurer(companyName, null);
        }

        model.addAttribute("success", success);
        model.addAttribute("error", error);
        model.addAttribute("role", role.toUpperCase());
        model.addAttribute("insurer", insurer);
        model.addAttribute("insPropsAndCars", insPropsAndCars);
        return "insurance/index";
    }

    @RequestMapping(value = "/", method = RequestMethod.POST)
    public String insuranceAccept(RedirectAttributes redirAttr,
                                  Authentication auth,
                                  @RequestParam String vin,
                                  @RequestParam String userToInsure) {
        String username = auth.getName();
        String role = userService.getRole(auth);

        ProfileProperties.User user = userService.findOrCreateUser(username, role);
        String company = user.getOrganization();

        try {
            carService.acceptInsurance(username, role, userToInsure, vin, company);
        } catch (Exception e) {
            if (redirAttr != null) {
                redirAttr.addAttribute("error", e.getMessage());
            }
        }

        redirAttr.addAttribute("success", "Insurance proposal accepted. '" + company + "' now insures car '" + vin + "' of user '" + userToInsure + "'.");

        return "redirect:/insurance/";
    }
}
