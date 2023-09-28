package io.paasas.pipelines.server.github.module;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import io.paasas.pipelines.server.github.domain.port.backend.AccessTokenRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GithubAuthenticationInterceptor implements ClientHttpRequestInterceptor {
	AccessTokenRepository accessTokenRepository;

	@Override
	public ClientHttpResponse intercept(
			HttpRequest request,
			byte[] body,
			ClientHttpRequestExecution execution)
			throws IOException {
		var updatedRequest = new HttpRequestWrapper(request);

		updatedRequest.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessTokenRepository.accessToken());

		return execution.execute(updatedRequest, body);
	}

}
