package io.paasas.pipelines.server.github.module.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.paasas.pipelines.server.github.domain.port.backend.AccessTokenRepository;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/api/ci/github/access-token")
public class AccessTokenController {
	record AccessTokenResponse(String accessToken) {

	}

	AccessTokenRepository accessTokenRepository;

	@PostMapping
	public AccessTokenResponse refreshAccessToken() {
		return new AccessTokenResponse(accessTokenRepository.refreshAccessToken());
	}
}
