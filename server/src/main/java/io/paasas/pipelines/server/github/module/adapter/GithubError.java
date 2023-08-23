package io.paasas.pipelines.server.github.module.adapter;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GithubError {
	String message;
	String documentationUrl;
}
