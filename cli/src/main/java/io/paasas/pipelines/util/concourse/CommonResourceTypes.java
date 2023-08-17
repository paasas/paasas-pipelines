package io.paasas.pipelines.util.concourse;

import io.paasas.pipelines.util.concourse.model.ResourceType;
import io.paasas.pipelines.util.concourse.model.ResourceTypeSource;

public final class CommonResourceTypes {
	public final static String GCS_RESOURCE_TYPE = "gcs";
	public static final String GIT_RESOURCE_TYPE = "git";
	public static final String METADATA_RESOURCE_TYPE = "metadata";
	public static final String REGISTRY_IMAGE_RESOURCE_TYPE = "registry-image";
	public final static String TEAMS_NOTIFICATION_RESOURCE_TYPE = "teams-notification";

	public static final ResourceType GCS = ResourceType.builder()
			.name(GCS_RESOURCE_TYPE)
			.source(ResourceTypeSource.builder()
					.repository("frodenas/gcs-resource")
					.tag("latest")
					.build())
			.type("docker-image")
			.build();
	
	public static final ResourceType METADATA = ResourceType.builder()
			.name(METADATA_RESOURCE_TYPE)
			.source(ResourceTypeSource.builder()
					.repository("olhtbr/metadata-resource")
					.tag("2.0.1")
					.build())
			.type("docker-image")
			.build();

	public final static ResourceType TEAMS_NOTIFICATION = ResourceType.builder()
			.name(TEAMS_NOTIFICATION_RESOURCE_TYPE)
			.source(ResourceTypeSource.builder()
					.repository("navicore/teams-notification-resource")
					.tag("latest")
					.build())
			.type("docker-image")
			.build();
}
