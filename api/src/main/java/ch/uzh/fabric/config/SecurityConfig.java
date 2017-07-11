package ch.uzh.fabric.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    public static final String BOOTSTRAP_GARAGE_USER = "garage";
    public static final String BOOTSTRAP_GARAGE_ROLE = "GARAGE";
    public static final String BOOTSTRAP_PRIVATE_USER = "user";
    public static final String BOOTSTRAP_PRIVATE_USER_ROLE = "USER";
    public static final String BOOTSTRAP_INSURANCE_USER = "insurance";
    public static final String BOOTSTRAP_INSURANCE_ROLE = "INSURANCE";
    public static final String BOOTSTRAP_DOT_USER = "dot";
    public static final String BOOTSTRAP_DOT_ROLE = "DOT";

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers("/css/**", "/index").permitAll()
                .antMatchers("/user/**").hasRole("USER")
                .antMatchers("/garage/**").hasRole("GARAGE")
                .antMatchers("/insurance/**").hasRole("INSURANCE")
                .antMatchers("/dot/**").hasRole("DOT")
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
