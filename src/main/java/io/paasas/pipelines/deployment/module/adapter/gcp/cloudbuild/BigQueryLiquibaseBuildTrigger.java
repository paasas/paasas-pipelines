package io.paasas.pipelines.deployment.module.adapter.gcp.cloudbuild;

import com.google.cloudbuild.v1.Build;
import com.google.cloudbuild.v1.BuildStep;
import com.google.cloudbuild.v1.BuildTrigger;

import io.paasas.pipelines.deployment.domain.model.BigQueryWatcher;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BigQueryLiquibaseBuildTrigger {
	public static final String TAG_BIGQUERY_LIQUIBASE = "paasas.io/cicd-component:bigquery-liquibase";

	public BuildTrigger buildTrigger(BigQueryWatcher watcher) {

		return BuildTrigger.newBuilder()
				.setName("big-query-liquibase-" + watcher.getDataset())
				.setBuild(Build.newBuilder()
						.addSteps(runLiquibase()))
				.addTags(TAG_BIGQUERY_LIQUIBASE)
				.build();
	}

//	private PushFilter.Builder pushFilter(BigQueryWatcher watcher) {
//		var pushFilter = PushFilter.newBuilder();
//
//		if (watcher.getGithub().getBranch() != null
//				&& !watcher.getGithub().getBranch().isBlank()) {
//			pushFilter.setBranch(String.format("^%s$", watcher.getGithub().getBranch()));
//		}
//
//		if (watcher.getGithub().getTag() != null
//				&& !watcher.getGithub().getTag().isBlank()) {
//			pushFilter.setTag(String.format("^%s$", watcher.getGithub().getTag()));
//		}
//
//		return pushFilter;
//	}

	private BuildStep.Builder runLiquibase() {
		return BuildStep.newBuilder()
				.setName("gcr.io/cloud-builders/git")
				.setEntrypoint("bash")
				.addArgs("-c")
				.addArgs("""
						echo "Running liquibase wot wot wot"
						""");
	}
}
