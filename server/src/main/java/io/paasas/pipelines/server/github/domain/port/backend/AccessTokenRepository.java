package io.paasas.pipelines.server.github.domain.port.backend;

public interface AccessTokenRepository {
	String accessToken();
	
	String refreshAccessToken();
}
