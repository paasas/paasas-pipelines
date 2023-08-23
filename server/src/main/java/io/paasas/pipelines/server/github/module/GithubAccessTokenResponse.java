package io.paasas.pipelines.server.github.module;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@AllArgsConstructor
@Builder(toBuilder = true)
public class GithubAccessTokenResponse {
	String token;

	@JsonProperty("expires_at")
	LocalDateTime expiresAt;

	Map<String, String> permissions;

	@JsonProperty("repository_selection")
	String repositorySelection;
}
