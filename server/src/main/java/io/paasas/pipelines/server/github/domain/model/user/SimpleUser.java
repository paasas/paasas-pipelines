package io.paasas.pipelines.server.github.domain.model.user;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Jacksonized
@Builder(toBuilder = true)
public class SimpleUser {
	String login;
	long id;
	String nodeId;
	String avatarUrl;
	String gravatarId;
	String url;
	String htmlUrl;
	String followersUrl;
	String followingUrl;
	String gistsUrl;
	String starredUrl;
	String subscriptionsUrl;
	String organizationsUrl;
	String reposUrl;
	String eventsUrl;
	String receivedEventsUrl;
	String type;
	boolean siteAdmin;
}
