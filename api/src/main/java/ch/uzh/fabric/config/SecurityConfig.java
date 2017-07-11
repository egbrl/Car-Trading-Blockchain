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
                .withUser("user").password("password").roles("USER");
        auth
                .inMemoryAuthentication()
                .withUser(BOOTSTRAP_GARAGE_USER).password("password").roles(BOOTSTRAP_GARAGE_ROLE);
        auth
                .inMemoryAuthentication()
                .withUser("insurance").password("password").roles("INSURANCE");
        auth
                .inMemoryAuthentication()
                .withUser("dot").password("password").roles("DOT");
    }
}
