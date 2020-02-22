package com.sam.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private TokenFilter tokenFilter;

	@Value("${saga-server.secured}")
	private Boolean enableSecurity;
	@Value("${saga-server.username}")
	private String username;
	@Value("${saga-server.password}")
	private String password;

	protected void configure(HttpSecurity http) throws Exception {
		if (enableSecurity) {
			http.authorizeRequests().anyRequest().authenticated().and().formLogin();
		} else {
			http.authorizeRequests().antMatchers("*/*").permitAll();
		}
		http.addFilterBefore(tokenFilter, UsernamePasswordAuthenticationFilter.class);
		http.csrf().disable();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		web.ignoring().antMatchers(HttpMethod.GET, "/js/*", "/css/*", "/images/*", "/favicon.ico", "/actuator/**").and()
				.ignoring().antMatchers("/error/**").and().ignoring().antMatchers(HttpMethod.POST, "/transaction/**");
		;
	}

	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder(11);
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication().withUser(username).password(this.encoder().encode(password)).roles("ADMIN");
	}
}
