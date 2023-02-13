package io.paasas.pipelines.cloudbuild.module.adapter;

import com.google.cloud.devtools.cloudbuild.v1.CloudBuildClient;
import com.google.cloudbuild.v1.Build;
import com.google.cloudbuild.v1.BuildTrigger;
import com.google.cloudbuild.v1.CreateBuildTriggerRequest;
import com.google.cloudbuild.v1.GitHubEventsConfig;
import com.google.cloudbuild.v1.Source;

public class CloudBuildGCloudClient {
	CloudBuildClient cloudBuildClient;

	public void createBuild(String projectId, String githubOwner, String githubRepository) {
		var request = CreateBuildTriggerRequest.newBuilder()
				.setProjectId(projectId)
				.setTrigger(BuildTrigger.newBuilder()
						.setGithub(GitHubEventsConfig.newBuilder()
								.setOwner(githubOwner)
								.setName(githubRepository)
								.build())
						.setBuild(Build.newBuilder()
								.setSource(Source.newBuilder()
										.build())
								.build())
						.build())
				.build();
	}
}
