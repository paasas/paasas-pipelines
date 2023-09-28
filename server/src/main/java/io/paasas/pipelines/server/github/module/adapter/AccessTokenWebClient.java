package io.paasas.pipelines.server.github.module.adapter;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.RestTemplate;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.paasas.pipelines.server.github.domain.port.backend.AccessTokenRepository;
import io.paasas.pipelines.server.github.module.GithubAccessTokenResponse;
import io.paasas.pipelines.server.github.module.GithubConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccessTokenWebClient implements AccessTokenRepository {
	private final GithubConfiguration githubConfiguration;
	private final RestTemplate restTemplate;

	private GithubAccessTokenResponse accessToken;
	private LocalDateTime tokenExpiration;

	public AccessTokenWebClient(GithubConfiguration githubConfiguration) {
		this.githubConfiguration = githubConfiguration;

		restTemplate = new RestTemplateBuilder()
				.rootUri(githubConfiguration.getBaseUrl())
				.build();
	}

	@Override
	public synchronized String accessToken() {
		if (tokenExpiration != null && tokenExpiration.isBefore(LocalDateTime.now())) {
			accessToken = null;
		}

		if (accessToken == null) {
			log.info("Renewing Github access token");

			tokenExpiration = LocalDateTime.now().plusHours(1).minusMinutes(5);

			this.accessToken = restTemplate
					.exchange(
							"/app/installations/{installationId}/access_tokens",
							HttpMethod.POST,
							new HttpEntity<Void>(
									new HttpHeaders(new MultiValueMapAdapter<>(Map.of(
											HttpHeaders.ACCEPT, List.of("application/vnd.github+json"),
											HttpHeaders.AUTHORIZATION, List.of("Bearer " + generateJwt()),
											"X-GitHub-Api-Version", List.of(githubConfiguration.getApiVersion()))))),
							GithubAccessTokenResponse.class,
							githubConfiguration.getInstallationId())
					.getBody();

			log.info("Successfully renewed Github access token");
		}

		return accessToken.getToken();
	}

	private String generateJwt() {
		long nowMillis = System.currentTimeMillis();

		return Jwts.builder()
				.setHeaderParam("typ", "JWT")
				.setId(null)
				.setIssuedAt(new Date(nowMillis))
				.setExpiration(new Date(nowMillis + 600))
				.setIssuer(githubConfiguration.getAppId())
				.signWith(
						SignatureAlgorithm.RS256,
						privateKey())
				.compact();
	}

	public PrivateKey privateKey() {
		try {
			return KeyFactory.getInstance("RSA")
					.generatePrivate(
							new PKCS8EncodedKeySpec(Base64.getDecoder().decode(new String(Base64.getDecoder()
									.decode(githubConfiguration.getPrivateKeyBase64().getBytes()))
									.replace("-----BEGIN PRIVATE KEY-----", "")
									.replace("-----END PRIVATE KEY-----", "")
									.replaceAll("\\s", ""))));
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public String refreshAccessToken() {
		accessToken = null;
		
		return accessToken();
	}
}
