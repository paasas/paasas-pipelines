package io.paasas.pipelines.platform.module.adapter.concourse;

import java.util.Map;
import java.util.TreeMap;

import io.paasas.pipelines.ConcourseConfiguration;
import io.paasas.pipelines.platform.module.adapter.concourse.model.step.Put;
import io.paasas.pipelines.platform.module.adapter.concourse.model.step.Step;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PROTECTED, makeFinal = true)
public abstract class ConcoursePipeline {
	ConcourseConfiguration configuration;

	private boolean isTeamsConfigured() {
		return configuration.getTeamsWebhookUrl() != null && !configuration.getTeamsWebhookUrl().isBlank();
	}

	Step teamsFailureNotification() {
		return isTeamsConfigured()
				? Put.builder()
						.put("teams")
						.params(new TreeMap<>(Map.of(
								"text",
								"Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME failed",
								"actionTarget",
								"$ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME")))
						.build()
				: null;
	}

	Step teamsSuccessNotification() {
		return isTeamsConfigured()
				? Put.builder()
						.put("teams")
						.params(new TreeMap<>(Map.of(
								"text",
								"Job ((concourse-url))/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME completed successfully",
								"actionTarget",
								"$ATC_EXTERNAL_URL/teams/$BUILD_TEAM_NAME/pipelines/$BUILD_PIPELINE_NAME/jobs/$BUILD_JOB_NAME/builds/$BUILD_NAME")))
						.build()
				: null;
	}
}
