package io.paasas.pipelines.server.github.domain.model.commit;

import java.time.LocalDateTime;

import io.paasas.pipelines.server.github.domain.model.user.SimpleUser;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class CommitStatus {
	String url;
	String avatarUrl;
	long id;
	String nodeId;
	CommitState state;
	String description;
	String targetUrl;
	String context;
	LocalDateTime createdAt;
	LocalDateTime updatedAt;
	SimpleUser creator;
}
