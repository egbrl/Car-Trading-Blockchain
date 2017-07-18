package ch.uzh.fabric.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties("profile")
public class ProfileProperties {
    private List<User> users = new ArrayList<>();

    public static class User {
        private String name;
        private String organization;
        private String role;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOrganization() {
            return organization;
        }

        public void setOrganization(String organization) {
            this.organization = organization;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public static String getOrganization(ProfileProperties pp, String username, String role) {
        List<ProfileProperties.User> users = pp.getUsers();
        String companyName = "UNKNOWN";
        for (ProfileProperties.User u : users) {
            if (u.getName().equals(username) && u.getRole().equals(role)) {
                companyName = u.getOrganization();
            }
        }

        return companyName;
    }
}
