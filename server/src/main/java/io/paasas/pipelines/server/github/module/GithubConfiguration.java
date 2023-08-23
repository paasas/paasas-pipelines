package io.paasas.pipelines.server.github.module;

import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Validated
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GithubConfiguration {
	@NotEmpty
	String apiVersion;

	@NotEmpty
	String appId;

	@NotEmpty
	String baseUrl;

	@NotEmpty
	String installationId;

	@NotEmpty
	String privateKeyBase64;
}
