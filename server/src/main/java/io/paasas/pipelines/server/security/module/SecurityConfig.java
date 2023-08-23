package io.paasas.pipelines.server.security.module;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

	@Bean
	@ConfigurationProperties("pipelines.security.ci")
	public CiSecurityConfiguration ciSecurityConfiguration() {
		return new CiSecurityConfiguration();
	}

	@Configuration
	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	class SecurityWebFilterChainConfig {
		CiSecurityConfiguration ciSecurityConfiguration;

		@Bean
		public SecurityFilterChain customSecurityFilterChain(HttpSecurity http) throws Exception {
			return http
					.csrf(csrf -> csrf.disable())
					.authorizeHttpRequests(authz -> customize(authz))
					.build();
		}

		private void customize(
				AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry authz) {
			authz
					.requestMatchers("/api/ci/**")
					.access(new CiAuthorizationManager(ciSecurityConfiguration.getUsers()))
					.requestMatchers("/api/**")
					.authenticated()
					.anyRequest()
					.permitAll();
		}
	}

	@AllArgsConstructor
	@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
	public class CiAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {
		List<CiUser> authorizedUsers;

		@Override
		public AuthorizationDecision check(Supplier<Authentication> authentication,
				RequestAuthorizationContext authorizationContext) {
			var authorizationEnum = authorizationContext
					.getRequest()
					.getHeaders(HttpHeaders.AUTHORIZATION);

			if (!authorizationEnum.hasMoreElements()) {
				return new AuthorizationDecision(false);
			}

			return new AuthorizationDecision(Optional.of(authorizationEnum.nextElement())
					.filter(authorization -> authorization.startsWith("Basic "))
					.map(authorization -> authorization.substring(6).trim())
					.filter(basic -> authorizedUsers.stream()
							.anyMatch(user -> basic.equals(HttpHeaders.encodeBasicAuth(
									user.getUsername(),
									user.getPassword(),
									null))))
					.isPresent());
		}
	}
}
