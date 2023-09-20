package io.paasas.pipelines.server.github.domain.model.pull;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CreatePullRequestComment {
	String body;
}
