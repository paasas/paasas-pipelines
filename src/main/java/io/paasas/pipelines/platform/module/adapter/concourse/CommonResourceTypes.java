package io.paasas.pipelines.platform.module.adapter.concourse;

import io.paasas.pipelines.platform.module.adapter.concourse.model.ResourceType;
import io.paasas.pipelines.platform.module.adapter.concourse.model.ResourceTypeSource;

public final class CommonResourceTypes {
	public static final String GIT_RESOURCE_TYPE = "git";
	public final static String TEAMS_NOTIFICATION_RESOURCE_TYPE = "teams-notification";

	public final static ResourceType TEAMS_NOTIFICATION = ResourceType.builder()
			.name(TEAMS_NOTIFICATION_RESOURCE_TYPE)
			.type("docker-image")
			.source(ResourceTypeSource.builder()
					.repository("navicore/teams-notification-resource")
					.tag("latest")
					.build())
			.build();
}
