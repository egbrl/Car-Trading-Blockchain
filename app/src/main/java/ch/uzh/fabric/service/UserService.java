package ch.uzh.fabric.service;

import ch.uzh.fabric.config.ProfileProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    @Autowired
    private ProfileProperties profileProperties;

    public ProfileProperties.User findOrCreateUser(String username, String role) {
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

        return user;
    }

    public String getRole(Authentication auth) {
        // removes the "ROLE_" prefix
        return auth.getAuthorities().toArray()[0].toString().substring(5);
    }
}
