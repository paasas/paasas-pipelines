package io.paasas.pipelines.server.github.module.adapter.model.pull;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CreatePullRequestComment {
	String body;
}
