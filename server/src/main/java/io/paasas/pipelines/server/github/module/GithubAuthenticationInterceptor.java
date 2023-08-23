package io.paasas.pipelines.server.github.module;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.client.RestTemplate;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class GithubAuthenticationInterceptor implements ClientHttpRequestInterceptor {
	private final GithubConfiguration githubConfiguration;
	private final RestTemplate restTemplate;

	private GithubAccessTokenResponse accessToken;

	public GithubAuthenticationInterceptor(GithubConfiguration githubConfiguration) {
		this.githubConfiguration = githubConfiguration;

		restTemplate = new RestTemplateBuilder()
				.rootUri(githubConfiguration.getBaseUrl())
				.build();
	}

	synchronized GithubAccessTokenResponse accessToken() {
		if (accessToken == null) {
			this.accessToken = restTemplate.exchange(
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
		}
		return accessToken;
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
			String privateKeyPEM = new String(
					Base64.getDecoder().decode(githubConfiguration.getPrivateKeyBase64().getBytes()));

			// strip of header, footer, newlines, whitespaces
			privateKeyPEM = privateKeyPEM
					.replace("-----BEGIN PRIVATE KEY-----", "")
					.replace("-----END PRIVATE KEY-----", "")
					.replaceAll("\\s", "");

			// decode to get the binary DER representation
			byte[] privateKeyDER = Base64.getDecoder().decode(privateKeyPEM);

			KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			PrivateKey privateKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyDER));
			return privateKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public ClientHttpResponse intercept(
			HttpRequest request,
			byte[] body,
			ClientHttpRequestExecution execution)
			throws IOException {
		var updatedRequest = new HttpRequestWrapper(request);

		updatedRequest.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken().getToken());

		return execution.execute(updatedRequest, body);
	}

}
