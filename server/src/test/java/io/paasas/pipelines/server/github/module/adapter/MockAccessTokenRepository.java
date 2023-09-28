package io.paasas.pipelines.server.github.module.adapter;

import java.util.UUID;

import io.paasas.pipelines.server.github.domain.port.backend.AccessTokenRepository;

public class MockAccessTokenRepository implements AccessTokenRepository {
	private static String ACCESS_TOKEN;

	@Override
	public synchronized String accessToken() {
		if (ACCESS_TOKEN == null) {
			ACCESS_TOKEN = UUID.randomUUID().toString();
		}

		return ACCESS_TOKEN;
	}

	@Override
	public String refreshAccessToken() {
		ACCESS_TOKEN = null;

		return accessToken();
	}

}
