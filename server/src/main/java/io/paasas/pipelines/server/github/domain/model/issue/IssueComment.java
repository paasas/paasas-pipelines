package io.paasas.pipelines.server.github.domain.model.issue;

import java.time.LocalDateTime;

import io.paasas.pipelines.server.github.domain.model.user.SimpleUser;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class IssueComment {
	String authorAssociation;
	String body;
	LocalDateTime createdAt;
	String htmlUrl;
	int id;
	String issueUrl;
	String nodeId;
	LocalDateTime updatedAt;
	String url;
	SimpleUser user;
}
