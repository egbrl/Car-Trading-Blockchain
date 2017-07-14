package ch.uzh.fabric.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String BOOTSTRAP_GARAGE_USER = "garage";
    public static final String BOOTSTRAP_GARAGE_ROLE = "garage";
    public static final String BOOTSTRAP_PRIVATE_USER = "user";
    public static final String BOOTSTRAP_PRIVATE_USER_ROLE = "user";
    public static final String BOOTSTRAP_INSURANCE_USER = "insurance";
    public static final String BOOTSTRAP_INSURANCE_ROLE = "insurance";
    public static final String BOOTSTRAP_DOT_USER = "dot";
    public static final String BOOTSTRAP_DOT_ROLE = "dot";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/css/**").permitAll()
                .antMatchers("/user/**").hasRole("user")
                .antMatchers("/garage/**", "/index").hasRole(BOOTSTRAP_GARAGE_ROLE)
                .antMatchers("/insurance/**").hasRole("insurance")
                .antMatchers("/dot/**").hasRole("dot")
                .and()
                .formLogin().loginPage("/login").failureUrl("/login-error");
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth
                .inMemoryAuthentication()
                .withUser(BOOTSTRAP_PRIVATE_USER).password("password").roles(BOOTSTRAP_PRIVATE_USER_ROLE);
        auth
                .inMemoryAuthentication()
                .withUser(BOOTSTRAP_GARAGE_USER).password("password").roles(BOOTSTRAP_GARAGE_ROLE);
        auth
                .inMemoryAuthentication()
                .withUser(BOOTSTRAP_INSURANCE_USER).password("password").roles(BOOTSTRAP_INSURANCE_ROLE);
        auth
                .inMemoryAuthentication()
                .withUser(BOOTSTRAP_DOT_USER).password("password").roles(BOOTSTRAP_DOT_ROLE);
    }
}