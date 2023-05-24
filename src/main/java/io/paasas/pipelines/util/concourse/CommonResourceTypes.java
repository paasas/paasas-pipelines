package io.paasas.pipelines.util.concourse;

import io.paasas.pipelines.util.concourse.model.ResourceType;
import io.paasas.pipelines.util.concourse.model.ResourceTypeSource;

public final class CommonResourceTypes {
	public final static String GCS_RESOURCE_TYPE = "gcs";
	public static final String GIT_RESOURCE_TYPE = "git";
	public static final String REGISTRY_IMAGE_RESOURCE_TYPE = "registry-image";
	public final static String TEAMS_NOTIFICATION_RESOURCE_TYPE = "teams-notification";

	public static final ResourceType GCS = ResourceType.builder()
			.type("docker-image")
			.name(GCS_RESOURCE_TYPE)
			.source(ResourceTypeSource.builder()
					.repository("frodenas/gcs-resource")
					.tag("latest")
					.build())
			.build();

	public final static ResourceType TEAMS_NOTIFICATION = ResourceType.builder()
			.name(TEAMS_NOTIFICATION_RESOURCE_TYPE)
			.type("docker-image")
			.source(ResourceTypeSource.builder()
					.repository("navicore/teams-notification-resource")
					.tag("latest")
					.build())
			.build();
}
