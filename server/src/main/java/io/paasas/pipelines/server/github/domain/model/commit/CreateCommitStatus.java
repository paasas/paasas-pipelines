package io.paasas.pipelines.server.github.domain.model.commit;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CreateCommitStatus {
	String context;
	String description;
	String targetUrl;
	CommitState state;
}